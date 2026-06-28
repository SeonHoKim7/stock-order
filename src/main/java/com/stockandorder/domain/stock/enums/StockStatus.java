package com.stockandorder.domain.stock.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 재고 현황 화면의 재고 상태 분류. quantity 와 safetyStock 의 관계로 파생되는 값이다(저장 컬럼 아님).
 *
 * 겹치지 않는 파티션:
 * - 품절(OUT_OF_STOCK): quantity == 0   (미달 조건과 겹치므로 가장 먼저 판정)
 * - 미달(SHORTAGE)    : 0 < quantity < safetyStock
 * - 정상(NORMAL)      : quantity >= safetyStock
 *
 * safetyStock == 0 인 상품은 미달이 성립하지 않으므로 정상/품절만 갖는다(안전재고 기준 미설정 = 미달 개념 없음).
 */
@Getter
@RequiredArgsConstructor
public enum StockStatus {
    NORMAL("정상"),
    SHORTAGE("미달"),
    OUT_OF_STOCK("품절");

    private final String label;
}
