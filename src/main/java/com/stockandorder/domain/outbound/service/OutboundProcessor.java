package com.stockandorder.domain.outbound.service;

import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.repository.MemberRepository;
import com.stockandorder.domain.outbound.dto.OutboundCreateRequest;
import com.stockandorder.domain.outbound.entity.Outbound;
import com.stockandorder.domain.outbound.entity.OutboundItem;
import com.stockandorder.domain.outbound.repository.OutboundRepository;
import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.domain.product.repository.ProductRepository;
import com.stockandorder.domain.stock.service.StockService;
import com.stockandorder.domain.supplier.entity.Supplier;
import com.stockandorder.domain.supplier.enums.SupplierType;
import com.stockandorder.domain.supplier.repository.SupplierRepository;
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
 * 출고 처리 1건의 트랜잭션 단위. 여러 Aggregate(Outbound/Stock/StockLog)를 조율한다.
 *
 * 입고와의 차이:
 * - 1: status가 없어 생성과 동시에 재고를 차감한다(요청/확정 2단계 없음).
 * - E-1: 보호할 누적 카운터(발주 receivedQuantity 같은)가 없어 낙관적 락이 불필요하다. 경합은 재고
 *   한 곳뿐이며 StockService의 비관적 락이 막는다. 재시도는 출고번호 UNIQUE 충돌만 대상으로 한다.
 *
 * 재시도는 이 클래스 밖(OutboundService)에서 새 트랜잭션으로 수행한다.
 */
@Service
@RequiredArgsConstructor
public class OutboundProcessor {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OutboundRepository outboundRepository;
    private final SupplierRepository supplierRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;

    @Transactional
    public Long createOnce(OutboundCreateRequest request, Long processorId) {
        // 1. 판매처 검증(A-4): 비활성 차단 + SALES/BOTH만 허용(PURCHASE 차단).
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SUPPLIER_NOT_FOUND));
        validateSellable(supplier);

        Member processor = findMember(processorId);

        // 2. 출고 항목 구성. 5: 한 출고서에 같은 상품을 두 줄로 적는 것을 차단한다(수기 실수 방어).
        //    unitPrice는 Product.salePrice(매출가)를 스냅샷한다(3).
        validateNoDuplicateProduct(request.getItems());
        List<OutboundItem> outboundItems = new ArrayList<>();
        for (OutboundCreateRequest.ItemRequest itemReq : request.getItems()) {
            Product product = findProduct(itemReq.getProductId());
            outboundItems.add(OutboundItem.create(product, itemReq.getQuantity(), product.getSalePrice()));
        }

        // 3. Outbound를 먼저 저장해 id를 확보한다(StockLog.referenceId에 넣어야 하는 저장 순서 의존성).
        //    flush로 번호 UNIQUE 충돌도 조기에 감지한다.
        String outboundNumber = generateOutboundNumber();
        Outbound outbound = Outbound.create(outboundNumber, supplier, processor,
                request.getOutboundDate(), request.getNote(), outboundItems);
        outboundRepository.save(outbound);
        outboundRepository.flush();

        // 4. 재고 차감. 재고 락은 StockService 안에서 잡히므로, 데드락 방지를 위해
        //    productId 오름차순으로 호출해 락 획득 순서를 고정한다.
        List<OutboundItem> lockOrder = outbound.getItems().stream()
                .sorted(Comparator.comparing(it -> it.getProduct().getProductId()))
                .toList();
        for (OutboundItem item : lockOrder) {
            stockService.decrease(item.getProduct().getProductId(), item.getQuantity(), outbound.getOutboundId());
        }

        return outbound.getOutboundId();
    }

    private void validateSellable(Supplier supplier) {
        if (!supplier.isActive()) {
            throw new BusinessException(ErrorCode.SUPPLIER_INACTIVE);
        }
        if (supplier.getSupplierType() == SupplierType.PURCHASE) {
            throw new BusinessException(ErrorCode.SUPPLIER_TYPE_INVALID);
        }
    }

    // 5: 한 출고 요청 안에 같은 상품(productId)이 두 번 이상 나타나면 차단한다.
    private void validateNoDuplicateProduct(List<OutboundCreateRequest.ItemRequest> items) {
        long distinct = items.stream()
                .map(OutboundCreateRequest.ItemRequest::getProductId)
                .distinct()
                .count();
        if (distinct != items.size()) {
            throw new BusinessException(ErrorCode.OUTBOUND_DUPLICATE_PRODUCT);
        }
    }

    // F-1: 발주/입고와 동일한 번호 전략(OUT-yyyyMMdd-NNN). 형식은 복붙, 충돌 재시도만 공통화 대상(절충).
    private String generateOutboundNumber() {
        String prefix = "OUT-" + LocalDate.now().format(DATE_FORMAT) + "-";
        return outboundRepository.findMaxOutboundNumberByPrefix(prefix)
                .map(max -> {
                    int seq = Integer.parseInt(max.substring(max.lastIndexOf("-") + 1));
                    return prefix + String.format("%03d", seq + 1);
                })
                .orElse(prefix + "001");
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
