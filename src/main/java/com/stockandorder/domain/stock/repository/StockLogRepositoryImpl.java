package com.stockandorder.domain.stock.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.stockandorder.domain.stock.dto.StockLogResponse;
import com.stockandorder.domain.stock.dto.StockLogSearchCondition;
import com.stockandorder.domain.stock.enums.StockChangeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

import static com.stockandorder.domain.member.entity.QMember.member;
import static com.stockandorder.domain.product.entity.QProduct.product;
import static com.stockandorder.domain.stock.entity.QStockLog.stockLog;

@RequiredArgsConstructor
public class StockLogRepositoryImpl implements StockLogRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<StockLogResponse> search(StockLogSearchCondition condition, Pageable pageable) {
        // 이력 한 줄에 상품명과 처리자 이름이 같이 보여야 한다. 둘 다 지연 로딩 연관이라 그냥 순회하면
        // 행 수만큼 추가 쿼리가 나가므로(N+1), 조인해서 필요한 컬럼만 프로젝션한다.
        // actor는 nullable이라 leftJoin이어야 한다 — inner join이면 처리자 없는 과거 로그가 통째로 사라진다.
        List<StockLogResponse> content = queryFactory
                .select(Projections.constructor(StockLogResponse.class,
                        stockLog.createdAt,
                        product.productId,
                        product.productCode,
                        product.name,
                        stockLog.changeType,
                        stockLog.changeQuantity,
                        stockLog.beforeQuantity,
                        stockLog.afterQuantity,
                        stockLog.referenceId,
                        stockLog.reason,
                        member.name
                ))
                .from(stockLog)
                .join(stockLog.product, product)
                .leftJoin(stockLog.actor, member)
                .where(
                        productEq(condition.getProductId()),
                        keywordContains(condition.getKeyword()),
                        changeTypeIn(condition.getChangeTypes()),
                        createdAfter(condition.getFromDate()),
                        createdBefore(condition.getToDate())
                )
                // 이력은 최신순이 기본이다. (product_id, created_at) 인덱스의 정렬 방향과도 맞다.
                .orderBy(stockLog.createdAt.desc(), stockLog.stockLogId.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(stockLog.count())
                .from(stockLog)
                .join(stockLog.product, product)
                .where(
                        productEq(condition.getProductId()),
                        keywordContains(condition.getKeyword()),
                        changeTypeIn(condition.getChangeTypes()),
                        createdAfter(condition.getFromDate()),
                        createdBefore(condition.getToDate())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression productEq(Long productId) {
        return productId != null ? stockLog.product.productId.eq(productId) : null;
    }

    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return product.name.contains(keyword).or(product.productCode.contains(keyword));
    }

    private BooleanExpression changeTypeIn(List<StockChangeType> changeTypes) {
        return CollectionUtils.isEmpty(changeTypes) ? null : stockLog.changeType.in(changeTypes);
    }

    private BooleanExpression createdAfter(LocalDate fromDate) {
        return fromDate != null ? stockLog.createdAt.goe(fromDate.atStartOfDay()) : null;
    }

    // 종료일 당일을 포함해야 하므로 다음 날 0시 미만으로 비교한다(23:59:59로 자르면 그날 마지막 1초가 샌다).
    private BooleanExpression createdBefore(LocalDate toDate) {
        return toDate != null ? stockLog.createdAt.lt(toDate.plusDays(1).atStartOfDay()) : null;
    }
}
