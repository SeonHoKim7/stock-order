package com.stockandorder.domain.inbound.service;

import com.stockandorder.domain.inbound.dto.InboundCreateRequest;
import com.stockandorder.domain.inbound.entity.Inbound;
import com.stockandorder.domain.inbound.entity.InboundItem;
import com.stockandorder.domain.inbound.repository.InboundRepository;
import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.repository.MemberRepository;
import com.stockandorder.domain.order.entity.PurchaseOrder;
import com.stockandorder.domain.order.entity.PurchaseOrderItem;
import com.stockandorder.domain.order.repository.PurchaseOrderRepository;
import com.stockandorder.domain.stock.service.StockService;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 입고 처리 1건의 트랜잭션 단위(D-3 원자성). 여러 Aggregate(Inbound/Stock/StockLog/PurchaseOrder)를
 * 조율하는 오케스트레이션을 담당한다(D-1).
 *
 * 재시도는 이 클래스 밖(InboundService)에서 새 트랜잭션으로 수행한다. 낙관적 락 충돌이 나면 현재
 * 트랜잭션은 rollback-only가 되므로, 같은 트랜잭션 안에서 재시도할 수 없기 때문이다.
 */
@Service
@RequiredArgsConstructor
public class InboundProcessor {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InboundRepository inboundRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final MemberRepository memberRepository;
    private final StockService stockService;

    @Transactional
    public Long createOnce(InboundCreateRequest request, Long processorId) {
        // 1. Load-Then-Lock: 락을 잡기 전에 발주·항목·상품을 한 번에 로딩하고 검증부터 끝낸다.
        //    findForReceipt는 항목/상품을 fetch join(B-2)하고 발주 version을 강제 증가시킨다(E-1 낙관적 락).
        PurchaseOrder order = purchaseOrderRepository.findForReceipt(request.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        order.validateReceivable();

        Member processor = findMember(processorId);

        // 2. 입고 항목 구성. 각 입고 항목은 이 발주의 발주 항목을 가리켜야 한다(A-3 검증).
        //    H-2: 한 입고서에 같은 발주 항목을 두 줄로 적는 것은 수기 입력 실수로 보고 차단한다.
        validateNoDuplicateOrderItem(request.getItems());
        List<InboundItem> inboundItems = new ArrayList<>();
        for (InboundCreateRequest.ItemRequest itemReq : request.getItems()) {
            PurchaseOrderItem orderItem = findOrderItem(order, itemReq.getOrderItemId());
            inboundItems.add(InboundItem.create(orderItem, itemReq.getQuantity()));
        }

        // 3. Inbound를 먼저 저장해 id를 확보한다(D-2: StockLog.referenceId에 넣어야 하는 저장 순서 의존성).
        //    flush로 번호 UNIQUE 충돌도 조기에 감지한다.
        String inboundNumber = generateInboundNumber();
        Inbound inbound = Inbound.create(inboundNumber, order, processor,
                request.getInboundDate(), request.getNote(), inboundItems);
        inboundRepository.save(inbound);
        inboundRepository.flush();

        // 4. 재고 반영. 재고 락은 StockService 안에서 잡히므로(G-1), 데드락 방지를 위해
        //    productId 오름차순으로 호출해 락 획득 순서를 고정한다.
        List<InboundItem> lockOrder = inbound.getItems().stream()
                .sorted(Comparator.comparing(it -> it.getOrderItem().getProduct().getProductId()))
                .toList();
        for (InboundItem item : lockOrder) {
            PurchaseOrderItem orderItem = item.getOrderItem();
            int quantity = item.getQuantity();

            // C-1/H-3: 초과 입고(완료 항목 포함) 금지 + receivedQuantity 누적(발주 항목 엔티티가 스스로 보호)
            orderItem.receive(quantity);

            // G-1/I-1: 재고 증가 + StockLog 기록(referenceId=inbound_id)을 StockService가 원자적으로 처리
            stockService.increase(orderItem.getProduct().getProductId(), quantity, inbound.getInboundId());
        }

        // 5. C-2: 입고 누적 결과로 발주 상태 재계산(전량 입고면 COMPLETED, 아니면 IN_PROGRESS)
        order.refreshStatusByReceipt();

        return inbound.getInboundId();
    }

    // F-1: 발주번호와 동일한 전략(IN-yyyyMMdd-NNN). 출고까지 만들어 셋이 같은지 확인된 뒤 공통화 검토.
    private String generateInboundNumber() {
        String prefix = "IN-" + LocalDate.now().format(DATE_FORMAT) + "-";
        return inboundRepository.findMaxInboundNumberByPrefix(prefix)
                .map(max -> {
                    int seq = Integer.parseInt(max.substring(max.lastIndexOf("-") + 1));
                    return prefix + String.format("%03d", seq + 1);
                })
                .orElse(prefix + "001");
    }

    // H-2: 한 입고 요청 안에 같은 발주 항목(orderItemId)이 두 번 이상 나타나면 차단한다.
    private void validateNoDuplicateOrderItem(List<InboundCreateRequest.ItemRequest> items) {
        long distinct = items.stream()
                .map(InboundCreateRequest.ItemRequest::getOrderItemId)
                .distinct()
                .count();
        if (distinct != items.size()) {
            throw new BusinessException(ErrorCode.INBOUND_DUPLICATE_ORDER_ITEM);
        }
    }

    private PurchaseOrderItem findOrderItem(PurchaseOrder order, Long orderItemId) {
        return order.getItems().stream()
                .filter(it -> it.getOrderItemId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
