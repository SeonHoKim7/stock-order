package com.stockandorder.domain.stock.dto;

import com.stockandorder.domain.stock.enums.StockStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockSearchCondition {

    // 상품명 또는 상품코드 키워드
    private String keyword;

    private Long categoryId;

    // 재고 상태 필터(정상/미달/품절). null이면 전체.
    private StockStatus status;

    // 정렬 옵션: "QUANTITY_ASC"(재고 적은 순) / "QUANTITY_DESC"(많은 순) / 그 외/없음=상품명순
    private String sort;
}
