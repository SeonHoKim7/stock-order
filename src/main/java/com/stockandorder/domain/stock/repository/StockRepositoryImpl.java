package com.stockandorder.domain.stock.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.stockandorder.domain.stock.dto.StockListResponse;
import com.stockandorder.domain.stock.dto.StockSearchCondition;
import com.stockandorder.domain.stock.enums.StockStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.stockandorder.domain.category.entity.QCategory.category;
import static com.stockandorder.domain.product.entity.QProduct.product;
import static com.stockandorder.domain.stock.entity.QStock.stock;

@RequiredArgsConstructor
public class StockRepositoryImpl implements StockRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<StockListResponse> search(StockSearchCondition condition, Pageable pageable) {
        // 읽기 전용 현황 조회: Stock⨝Product(⨝Category) 조인 후 필요한 컬럼만 프로젝션(N+1 차단).
        List<StockListResponse> content = queryFactory
                .select(Projections.constructor(StockListResponse.class,
                        product.productId,
                        product.productCode,
                        product.name,
                        category.name,
                        stock.quantity,
                        product.safetyStock
                ))
                .from(stock)
                .join(stock.product, product)
                .leftJoin(product.category, category)
                .where(
                        keywordContains(condition.getKeyword()),
                        categoryEq(condition.getCategoryId()),
                        statusEq(condition.getStatus())
                )
                .orderBy(orderSpecifier(condition.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(stock.count())
                .from(stock)
                .join(stock.product, product)
                .where(
                        keywordContains(condition.getKeyword()),
                        categoryEq(condition.getCategoryId()),
                        statusEq(condition.getStatus())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return product.name.containsIgnoreCase(keyword)
                .or(product.productCode.containsIgnoreCase(keyword));
    }

    private BooleanExpression categoryEq(Long categoryId) {
        return categoryId != null ? product.category.categoryId.eq(categoryId) : null;
    }

    // 상태 필터는 반드시 WHERE에서 처리해야 LIMIT(페이징)이 정확해진다(애플리케이션 필터링 금지).
    // 미달은 safetyStock(상품마다 다른 기준) 대비 상대값이므로 단순 컬럼 비교가 아니다.
    private BooleanExpression statusEq(StockStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case OUT_OF_STOCK -> stock.quantity.eq(0);
            case SHORTAGE -> stock.quantity.gt(0).and(stock.quantity.lt(product.safetyStock));
            case NORMAL -> stock.quantity.goe(product.safetyStock);
        };
    }

    // 기본 정렬은 상품명순(중립·예측가능). 위험 노출은 상태 필터가 담당한다.
    private OrderSpecifier<?> orderSpecifier(String sort) {
        if ("QUANTITY_ASC".equals(sort)) {
            return stock.quantity.asc();
        }
        if ("QUANTITY_DESC".equals(sort)) {
            return stock.quantity.desc();
        }
        return product.name.asc();
    }
}
