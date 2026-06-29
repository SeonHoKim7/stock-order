package com.stockandorder.domain.stock.dto;

import lombok.Getter;

/**
 * 재고 수동 조정 폼에 표시할 현재 재고 정보(읽기 전용).
 * currentQuantity는 폼에 표시됨과 동시에 hidden seenQuantity로 제출되어 낙관적 검증의 기준값이 된다.
 */
@Getter
public class StockAdjustFormResponse {

    private final Long productId;
    private final String productCode;
    private final String productName;
    private final int currentQuantity;

    public StockAdjustFormResponse(Long productId, String productCode,
                                   String productName, int currentQuantity) {
        this.productId = productId;
        this.productCode = productCode;
        this.productName = productName;
        this.currentQuantity = currentQuantity;
    }
}
