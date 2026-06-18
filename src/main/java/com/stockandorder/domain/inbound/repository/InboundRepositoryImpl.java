package com.stockandorder.domain.inbound.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.stockandorder.domain.inbound.dto.InboundListResponse;
import com.stockandorder.domain.inbound.dto.InboundSearchCondition;
import com.stockandorder.domain.member.entity.QMember;
import com.stockandorder.domain.order.entity.QPurchaseOrder;
import com.stockandorder.domain.supplier.entity.QSupplier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDate;
import java.util.List;

import static com.stockandorder.domain.inbound.entity.QInbound.inbound;

@RequiredArgsConstructor
public class InboundRepositoryImpl implements InboundRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QPurchaseOrder order = new QPurchaseOrder("order");
    private static final QSupplier supplier = new QSupplier("supplier");
    private static final QMember processor = new QMember("processor");

    @Override
    public Page<InboundListResponse> search(InboundSearchCondition condition, Pageable pageable) {
        // B-1: supplier는 inbound에 중복 보관하지 않고 inbound → order → supplier 로 조인해 조회한다.
        List<InboundListResponse> content = queryFactory
                .select(Projections.constructor(InboundListResponse.class,
                        inbound.inboundId,
                        inbound.inboundNumber,
                        order.orderNumber,
                        supplier.name,
                        processor.name,
                        inbound.inboundDate
                ))
                .from(inbound)
                .join(inbound.purchaseOrder, order)
                .join(order.supplier, supplier)
                .join(inbound.processor, processor)
                .where(
                        supplierIdEq(condition.getSupplierId()),
                        inboundDateGoe(condition.getStartDate()),
                        inboundDateLoe(condition.getEndDate())
                )
                .orderBy(inbound.inboundDate.desc(), inbound.inboundId.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(inbound.count())
                .from(inbound)
                .join(inbound.purchaseOrder, order)
                .join(order.supplier, supplier)
                .where(
                        supplierIdEq(condition.getSupplierId()),
                        inboundDateGoe(condition.getStartDate()),
                        inboundDateLoe(condition.getEndDate())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression supplierIdEq(Long supplierId) {
        return supplierId != null ? order.supplier.supplierId.eq(supplierId) : null;
    }

    private BooleanExpression inboundDateGoe(LocalDate startDate) {
        return startDate != null ? inbound.inboundDate.goe(startDate) : null;
    }

    private BooleanExpression inboundDateLoe(LocalDate endDate) {
        return endDate != null ? inbound.inboundDate.loe(endDate) : null;
    }
}
