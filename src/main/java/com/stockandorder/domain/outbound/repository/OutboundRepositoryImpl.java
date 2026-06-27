package com.stockandorder.domain.outbound.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.stockandorder.domain.member.entity.QMember;
import com.stockandorder.domain.outbound.dto.OutboundListResponse;
import com.stockandorder.domain.outbound.dto.OutboundSearchCondition;
import com.stockandorder.domain.supplier.entity.QSupplier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDate;
import java.util.List;

import static com.stockandorder.domain.outbound.entity.QOutbound.outbound;

@RequiredArgsConstructor
public class OutboundRepositoryImpl implements OutboundRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QSupplier supplier = new QSupplier("supplier");
    private static final QMember processor = new QMember("processor");

    @Override
    public Page<OutboundListResponse> search(OutboundSearchCondition condition, Pageable pageable) {
        // A-2: 출고는 supplier를 직접 참조하므로 outbound → supplier 로 바로 조인한다(발주 경유 없음).
        List<OutboundListResponse> content = queryFactory
                .select(Projections.constructor(OutboundListResponse.class,
                        outbound.outboundId,
                        outbound.outboundNumber,
                        supplier.name,
                        processor.name,
                        outbound.totalAmount,
                        outbound.outboundDate
                ))
                .from(outbound)
                .join(outbound.supplier, supplier)
                .join(outbound.processor, processor)
                .where(
                        supplierIdEq(condition.getSupplierId()),
                        outboundDateGoe(condition.getStartDate()),
                        outboundDateLoe(condition.getEndDate())
                )
                .orderBy(outbound.outboundDate.desc(), outbound.outboundId.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(outbound.count())
                .from(outbound)
                .join(outbound.supplier, supplier)
                .where(
                        supplierIdEq(condition.getSupplierId()),
                        outboundDateGoe(condition.getStartDate()),
                        outboundDateLoe(condition.getEndDate())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression supplierIdEq(Long supplierId) {
        return supplierId != null ? outbound.supplier.supplierId.eq(supplierId) : null;
    }

    private BooleanExpression outboundDateGoe(LocalDate startDate) {
        return startDate != null ? outbound.outboundDate.goe(startDate) : null;
    }

    private BooleanExpression outboundDateLoe(LocalDate endDate) {
        return endDate != null ? outbound.outboundDate.loe(endDate) : null;
    }
}
