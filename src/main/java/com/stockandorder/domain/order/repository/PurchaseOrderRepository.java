package com.stockandorder.domain.order.repository;

import com.stockandorder.domain.order.entity.PurchaseOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>, PurchaseOrderRepositoryCustom {

    @Query("SELECT MAX(po.orderNumber) FROM PurchaseOrder po WHERE po.orderNumber LIKE :prefix%")
    Optional<String> findMaxOrderNumberByPrefix(@Param("prefix") String prefix);

    /**
     * 입고 처리용 발주 조회.
     * - 항목/상품을 fetch join 으로 한 번에 로딩(B-2: 재고·검증에 필요한 product를 N+1 없이 확보)
     * - OPTIMISTIC_FORCE_INCREMENT: 자식(항목)의 receivedQuantity만 바뀌어도 루트 발주의 version을
     *   강제 증가시켜, 같은 발주에 대한 동시 입고를 낙관적 락으로 감지(E-1).
     */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT DISTINCT po FROM PurchaseOrder po " +
            "JOIN FETCH po.items i JOIN FETCH i.product " +
            "WHERE po.orderId = :orderId")
    Optional<PurchaseOrder> findForReceipt(@Param("orderId") Long orderId);
}
