package com.stockandorder.domain.stock.dto;

import com.stockandorder.domain.stock.enums.StockStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StockSearchCondition {

    // 상품명 또는 상품코드 키워드
    private String keyword;

    private Long categoryId;

    // 재고 상태 필터(다중선택). 비거나 null이면 전체.
    // 단일 enum이 아니라 목록인 이유: "품절+미달 동시 보기"를 표현하려면 상태들의 '집합'이 필요하다.
    // StockStatus enum 자체는 겹치지 않는 3분류로 두고(경고 같은 4번째 값 추가 금지),
    // "여러 상태를 한 번에 본다"는 책임은 필터(이 목록)가 진다.
    private List<StockStatus> statuses;

    // 정렬 옵션:
    //  - "QUANTITY_ASC"/"QUANTITY_DESC": 재고량 오름/내림차순
    //  - "RISK": 위험도순(품절 먼저 → 미달은 충족률 오름차순). 안전 재고 미달 경고 목록 기본 정렬.
    //  - 그 외/없음: 상품명순
    private String sort;
}
