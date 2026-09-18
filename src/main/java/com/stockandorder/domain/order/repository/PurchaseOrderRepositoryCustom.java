package com.stockandorder.domain.order.repository;

import com.stockandorder.domain.order.dto.PurchaseOrderListResponse;
import com.stockandorder.domain.order.dto.PurchaseOrderSearchCondition;
import com.stockandorder.domain.order.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PurchaseOrderRepositoryCustom {

    Page<PurchaseOrderListResponse> search(PurchaseOrderSearchCondition condition, Pageable pageable);

    /**
     * 입고 처리용 발주 조회.
     * - 항목/상품을 fetch join 으로 로딩(B-2, N+1 방지)
     * - 루트 발주의 version 만 OPTIMISTIC_FORCE_INCREMENT 로 강제 증가(E-1). 락을 fetch join 쿼리에
     *   함께 걸면 비버전 자식(PurchaseOrderItem)까지 락이 번져 실패하므로, 락은 루트에만 별도로 건다.
     */
    Optional<PurchaseOrder> findForReceipt(Long orderId);
}
