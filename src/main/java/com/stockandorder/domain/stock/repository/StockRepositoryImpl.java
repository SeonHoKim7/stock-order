package com.stockandorder.domain.stock.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
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
                        statusIn(condition.getStatuses())
                )
                .orderBy(orderSpecifiers(condition.getSort()))
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
                        statusIn(condition.getStatuses())
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
    // 다중선택: 선택된 상태들을 OR로 묶는다. "품절+미달 동시 보기"가 곧 안전 재고 경고 목록이다.
    // 비거나 null이면 필터 미적용(전체).
    private BooleanExpression statusIn(List<StockStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        BooleanExpression predicate = null;
        for (StockStatus status : statuses) {
            BooleanExpression each = statusPredicate(status);
            predicate = (predicate == null) ? each : predicate.or(each);
        }
        return predicate;
    }

    // 단일 상태의 조건식. 미달은 safetyStock(상품마다 다른 기준) 대비 상대값이라 단순 컬럼 비교가 아니다.
    private BooleanExpression statusPredicate(StockStatus status) {
        return switch (status) {
            case OUT_OF_STOCK -> stock.quantity.eq(0);
            case SHORTAGE -> stock.quantity.gt(0).and(stock.quantity.lt(product.safetyStock));
            // 수량 0은 안전재고와 무관하게 항상 품절(DTO getStatus 규칙과 일치). gt(0)을 빼면
            // 안전재고 0·수량 0인 상품이 NORMAL에도 잡혀 품절과 겹친다.
            case NORMAL -> stock.quantity.gt(0).and(stock.quantity.goe(product.safetyStock));
        };
    }

    // 정렬. 마지막에 productId를 붙여 동률 시 순서를 고정한다(페이징 시 행이 페이지 사이를 넘나드는 것 방지).
    private OrderSpecifier<?>[] orderSpecifiers(String sort) {
        OrderSpecifier<?> primary = switch (sort == null ? "" : sort) {
            case "QUANTITY_ASC" -> stock.quantity.asc();
            case "QUANTITY_DESC" -> stock.quantity.desc();
            // 위험도순: 안전 재고 경고 목록 기본 정렬. 아래 두 식을 차례로 적용한다.
            case "RISK" -> null;
            default -> product.name.asc();
        };

        if ("RISK".equals(sort)) {
            return new OrderSpecifier<?>[]{
                    outOfStockFirst().asc(),   // 1) 품절(수량 0) 블록을 맨 위로
                    fillRatio().asc(),         // 2) 미달은 충족률(현재/안전재고) 낮은 순 = 위급한 순
                    stock.product.productId.asc()
            };
        }
        return new OrderSpecifier<?>[]{primary, stock.product.productId.asc()};
    }

    // 품절(수량 0)이면 0, 아니면 1 → 오름차순 시 품절이 먼저 온다.
    private NumberExpression<Integer> outOfStockFirst() {
        return new CaseBuilder()
                .when(stock.quantity.eq(0)).then(0)
                .otherwise(1);
    }

    // 충족률 = 현재고 / 안전재고. 정렬식은 품절 행에도 평가되므로 두 가지를 반드시 가드한다:
    //  - 정수 나눗셈 잘림 방지: doubleValue()로 캐스팅 후 나눈다(90/100, 2/3가 둘 다 0이 되는 문제 차단).
    //  - 0 나눗셈 방지: 안전재고 0이면 0으로 둔다(미달은 정의상 safetyStock>0이라 안전하지만,
    //    같은 결과셋에 섞인 품절·안전재고0 행에서 0/0이 터지는 것을 막는다).
    private NumberExpression<Double> fillRatio() {
        return new CaseBuilder()
                .when(product.safetyStock.eq(0)).then(0.0)
                .otherwise(stock.quantity.doubleValue().divide(product.safetyStock.doubleValue()));
    }
}
