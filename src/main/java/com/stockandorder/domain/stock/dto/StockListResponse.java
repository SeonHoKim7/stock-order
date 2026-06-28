package com.stockandorder.domain.stock.dto;

import com.stockandorder.domain.stock.enums.StockStatus;
import lombok.Getter;

/**
 * 재고 현황 목록 한 줄. QueryDSL 프로젝션 대상(읽기 전용)이라 필요한 컬럼만 담는다.
 * 재고 상태/미달 여부는 quantity·safetyStock 에서 파생 계산한다(저장값 아님).
 */
@Getter
public class StockListResponse {

    private final Long productId;
    private final String productCode;
    private final String productName;
    private final String categoryName;
    private final int quantity;
    private final int safetyStock;

    public StockListResponse(Long productId, String productCode, String productName,
                             String categoryName, int quantity, int safetyStock) {
        this.productId = productId;
        this.productCode = productCode;
        this.productName = productName;
        this.categoryName = categoryName;
        this.quantity = quantity;
        this.safetyStock = safetyStock;
    }

    // 표시용 파생값: 품절을 미달보다 먼저 판정해 겹침을 차단한다(StockStatus 파티션 규칙).
    public StockStatus getStatus() {
        if (quantity == 0) {
            return StockStatus.OUT_OF_STOCK;
        }
        if (quantity < safetyStock) {
            return StockStatus.SHORTAGE;
        }
        return StockStatus.NORMAL;
    }

    public String getStatusLabel() {
        return getStatus().getLabel();
    }

    // 미달 또는 품절이면 true → 뷰에서 강조 표시
    public boolean isShortage() {
        return getStatus() != StockStatus.NORMAL;
    }
}
