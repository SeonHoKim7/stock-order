package com.stockandorder.domain.order.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.stockandorder.domain.member.entity.QMember;
import com.stockandorder.domain.order.dto.PurchaseOrderListResponse;
import com.stockandorder.domain.order.dto.PurchaseOrderSearchCondition;
import com.stockandorder.domain.order.enums.OrderStatus;
import com.stockandorder.domain.supplier.entity.QSupplier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDate;
import java.util.List;

import static com.stockandorder.domain.order.entity.QPurchaseOrder.purchaseOrder;

@RequiredArgsConstructor
public class PurchaseOrderRepositoryImpl implements PurchaseOrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QSupplier supplier = new QSupplier("supplier");
    private static final QMember requester = new QMember("requester");

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
