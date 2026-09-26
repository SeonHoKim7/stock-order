package com.stockandorder.domain.stock.dto;

import com.stockandorder.domain.stock.enums.StockChangeType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class StockLogSearchCondition {

    // 특정 상품으로 좁힐 때 사용한다. 재고 현황 목록의 "이력" 링크가 이 값을 채워서 넘어온다.
    // 이 값이 있으면 (product_id, created_at) 복합 인덱스를 그대로 타는 조회가 된다.
    private Long productId;

    // 상품명 또는 상품코드 키워드(상품을 지정하지 않고 넓게 볼 때)
    private String keyword;

    // 변동 유형 필터(다중선택). 비거나 null이면 전체.
    // 재고 상태 필터와 같은 이유로 단일 값이 아닌 목록이다 — "입고+출고만 보기" 같은 집합을 표현해야 한다.
    private List<StockChangeType> changeTypes;

    // 조회 기간(둘 다 선택). created_at 기준이며 종료일은 그날 24시까지 포함한다.
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;
}
