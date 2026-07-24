package com.stockandorder.domain.order.service;

import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.repository.MemberRepository;
import com.stockandorder.domain.order.dto.PurchaseOrderCreateRequest;
import com.stockandorder.domain.order.entity.PurchaseOrder;
import com.stockandorder.domain.order.entity.PurchaseOrderItem;
import com.stockandorder.domain.order.repository.PurchaseOrderRepository;
import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.domain.product.repository.ProductRepository;
import com.stockandorder.domain.supplier.entity.Supplier;
import com.stockandorder.domain.supplier.enums.SupplierType;
import com.stockandorder.domain.supplier.repository.SupplierRepository;
import com.stockandorder.global.common.DocumentNumberGenerator;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 발주 생성 1건의 트랜잭션 단위.
 *
 * 재시도는 이 클래스 밖(PurchaseOrderService)에서 새 트랜잭션으로 수행한다. 발주번호 UNIQUE 충돌이
 * 나면 현재 트랜잭션은 rollback-only가 되고 영속성 컨텍스트도 신뢰할 수 없는 상태가 되므로,
 * 같은 트랜잭션 안에서 번호만 바꿔 다시 저장하는 방식은 커밋 시점에 실패한다.
 * 입고(InboundProcessor)·출고(OutboundProcessor)와 동일한 구조다.
 */
@Service
@RequiredArgsConstructor
public class PurchaseOrderProcessor {

    private static final String DOCUMENT_PREFIX = "PO";

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final DocumentNumberGenerator documentNumberGenerator;

    @Transactional
    public Long createOnce(PurchaseOrderCreateRequest request, Long requesterId) {
        Supplier supplier = findSupplier(request.getSupplierId());
        validateSupplierForPurchase(supplier);

        Member requester = findMember(requesterId);

        String orderNumber = documentNumberGenerator.generate(
                DOCUMENT_PREFIX, purchaseOrderRepository::findMaxOrderNumberByPrefix);
        PurchaseOrder order = PurchaseOrder.create(orderNumber, supplier, requester, request.getNote());

        for (PurchaseOrderCreateRequest.ItemRequest itemReq : request.getItems()) {
            Product product = findProduct(itemReq.getProductId());
            // 발주 시점의 매입가를 스냅샷한다(이후 상품 가격이 바뀌어도 발주 금액은 보존).
            PurchaseOrderItem item = PurchaseOrderItem.create(
                    product,
                    itemReq.getQuantity(),
                    product.getPurchasePrice()
            );
            order.addItem(item);
        }

        // flush로 발주번호 UNIQUE 충돌을 커밋 전에 감지한다(입고·출고와 동일).
        purchaseOrderRepository.save(order);
        purchaseOrderRepository.flush();

        return order.getOrderId();
    }

    /** 발주 대상은 매입처(PURCHASE) 또는 겸용(BOTH)만 허용한다. 판매처(SALES)로는 발주할 수 없다. */
    private void validateSupplierForPurchase(Supplier supplier) {
        if (!supplier.isActive()) {
            throw new BusinessException(ErrorCode.SUPPLIER_INACTIVE);
        }
        if (supplier.getSupplierType() == SupplierType.SALES) {
            throw new BusinessException(ErrorCode.SUPPLIER_TYPE_INVALID);
        }
    }

    private Supplier findSupplier(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUPPLIER_NOT_FOUND));
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
