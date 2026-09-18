package com.stockandorder.domain.order.repository;

import com.stockandorder.domain.order.entity.PurchaseOrder;
import com.stockandorder.domain.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>, PurchaseOrderRepositoryCustom {

    @Query("SELECT MAX(po.orderNumber) FROM PurchaseOrder po WHERE po.orderNumber LIKE :prefix%")
    Optional<String> findMaxOrderNumberByPrefix(@Param("prefix") String prefix);

    // 대시보드 "승인 대기 발주" 위젯용. 상태 카운트는 시점과 무관한 현재 집계라 날짜 경계가 없다.
    long countByStatus(OrderStatus status);

    // 입고 처리용 발주 조회(findForReceipt)는 PurchaseOrderRepositoryCustom 으로 옮겼다.
    // 이유: OPTIMISTIC_FORCE_INCREMENT 락과 컬렉션 fetch join 을 한 쿼리에 함께 걸면,
    // fetch join 으로 함께 로딩되는 PurchaseOrderItem(@Version 없음)에까지 락이 적용되어
    // "not supported for non-versioned entities" 오류가 난다. 락을 루트에만 거는 2단계 구현이 필요하다.
}
