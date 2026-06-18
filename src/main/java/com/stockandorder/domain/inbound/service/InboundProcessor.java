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
import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.domain.stock.entity.Stock;
import com.stockandorder.domain.stock.entity.StockLog;
import com.stockandorder.domain.stock.enums.StockChangeType;
import com.stockandorder.domain.stock.repository.StockLogRepository;
import com.stockandorder.domain.stock.repository.StockRepository;
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
    private final StockRepository stockRepository;
    private final StockLogRepository stockLogRepository;

    @Transactional
    public Long createOnce(InboundCreateRequest request, Long processorId) {
        // 1. Load-Then-Lock: 락을 잡기 전에 발주·항목·상품을 한 번에 로딩하고 검증부터 끝낸다.
        //    findForReceipt는 항목/상품을 fetch join(B-2)하고 발주 version을 강제 증가시킨다(E-1 낙관적 락).
        PurchaseOrder order = purchaseOrderRepository.findForReceipt(request.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        order.validateReceivable();

        Member processor = findMember(processorId);

        // 2. 입고 항목 구성. 각 입고 항목은 이 발주의 발주 항목을 가리켜야 한다(A-3 검증).
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

        // 4. 재고 반영. 데드락 방지를 위해 재고 락은 항상 product_id 오름차순으로 획득한다.
        List<InboundItem> lockOrder = inbound.getItems().stream()
                .sorted(Comparator.comparing(it -> it.getOrderItem().getProduct().getProductId()))
                .toList();
        for (InboundItem item : lockOrder) {
            PurchaseOrderItem orderItem = item.getOrderItem();
            Product product = orderItem.getProduct();
            int quantity = item.getQuantity();

            // C-1: 초과 입고 금지 불변식 + receivedQuantity 누적(발주 항목 엔티티가 스스로 보호)
            orderItem.receive(quantity);

            // E-1: 재고는 핫 로우라 비관적 락으로 Lost Update 방지
            Stock stock = stockRepository.findByProductIdForUpdate(product.getProductId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));
            int before = stock.getQuantity();
            stock.increase(quantity);
            int after = stock.getQuantity();

            // D-2: 재고 변동 이력 기록. referenceId에 방금 확보한 inbound_id 주입.
            StockLog log = StockLog.of(product, StockChangeType.INBOUND,
                    quantity, before, after, inbound.getInboundId(), null);
            stockLogRepository.save(log);
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
