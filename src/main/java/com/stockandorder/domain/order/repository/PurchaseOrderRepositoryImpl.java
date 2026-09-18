package com.stockandorder.domain.order.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.stockandorder.domain.member.entity.QMember;
import com.stockandorder.domain.order.dto.PurchaseOrderListResponse;
import com.stockandorder.domain.order.dto.PurchaseOrderSearchCondition;
import com.stockandorder.domain.order.entity.PurchaseOrder;
import com.stockandorder.domain.order.enums.OrderStatus;
import com.stockandorder.domain.supplier.entity.QSupplier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.stockandorder.domain.order.entity.QPurchaseOrder.purchaseOrder;

@RequiredArgsConstructor
public class PurchaseOrderRepositoryImpl implements PurchaseOrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager em;

    private static final QSupplier supplier = new QSupplier("supplier");
    private static final QMember requester = new QMember("requester");

    /**
     * 입고 처리용 발주 조회(2단계).
     * 1) 항목/상품을 fetch join 으로 로딩해 재고 반영·검증에 필요한 데이터를 N+1 없이 확보한다(B-2).
     *    이 단계에는 락을 걸지 않는다 — 컬렉션 fetch join 쿼리에 OPTIMISTIC_FORCE_INCREMENT 를 함께 걸면
     *    fetch join 으로 함께 로딩되는 PurchaseOrderItem(@Version 없음)에까지 락이 적용되어 실패한다.
     * 2) 루트 발주에만 OPTIMISTIC_FORCE_INCREMENT 를 걸어 version 을 강제 증가시킨다(E-1). 자식(항목)의
     *    receivedQuantity 만 바뀌어도 루트 version 이 올라, 같은 발주에 대한 동시 입고를 낙관적 락으로 감지한다.
     */
    @Override
    public Optional<PurchaseOrder> findForReceipt(Long orderId) {
        List<PurchaseOrder> result = em.createQuery(
                        "SELECT DISTINCT po FROM PurchaseOrder po " +
                                "JOIN FETCH po.items i JOIN FETCH i.product " +
                                "WHERE po.orderId = :orderId", PurchaseOrder.class)
                .setParameter("orderId", orderId)
                .getResultList();
        if (result.isEmpty()) {
            return Optional.empty();
        }
        PurchaseOrder order = result.get(0);
        em.lock(order, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        return Optional.of(order);
    }

    @Override
    public Page<PurchaseOrderListResponse> search(PurchaseOrderSearchCondition condition, Pageable pageable) {
        List<PurchaseOrderListResponse> content = queryFactory
                .select(Projections.constructor(PurchaseOrderListResponse.class,
                        purchaseOrder.orderId,
                        purchaseOrder.orderNumber,
                        supplier.name,
                        requester.name,
                        purchaseOrder.status,
                        purchaseOrder.totalAmount,
                        purchaseOrder.orderedAt
                ))
                .from(purchaseOrder)
                .join(purchaseOrder.supplier, supplier)
                .join(purchaseOrder.requester, requester)
                .where(
                        statusEq(condition.getStatus()),
                        orderedAtGoe(condition.getStartDate()),
                        orderedAtLoe(condition.getEndDate()),
                        supplierIdEq(condition.getSupplierId())
                )
                .orderBy(purchaseOrder.orderedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(purchaseOrder.count())
                .from(purchaseOrder)
                .where(
                        statusEq(condition.getStatus()),
                        orderedAtGoe(condition.getStartDate()),
                        orderedAtLoe(condition.getEndDate()),
                        supplierIdEq(condition.getSupplierId())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression statusEq(OrderStatus status) {
        return status != null ? purchaseOrder.status.eq(status) : null;
    }

    private BooleanExpression orderedAtGoe(LocalDate startDate) {
        return startDate != null ? purchaseOrder.orderedAt.goe(startDate.atStartOfDay()) : null;
    }

    private BooleanExpression orderedAtLoe(LocalDate endDate) {
        return endDate != null ? purchaseOrder.orderedAt.loe(endDate.atTime(23, 59, 59)) : null;
    }

    private BooleanExpression supplierIdEq(Long supplierId) {
        return supplierId != null ? purchaseOrder.supplier.supplierId.eq(supplierId) : null;
    }
}
