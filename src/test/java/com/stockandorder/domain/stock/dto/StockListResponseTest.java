package com.stockandorder.domain.stock.dto;

import com.stockandorder.domain.stock.enums.StockStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 재고 상태(품절/미달/정상) 파생 로직 단위 테스트.
 * quantity·safetyStock 조합만으로 판정되는 순수 계산이라 Mock 없이 검증한다.
 */
class StockListResponseTest {

    private StockListResponse response(int quantity, int safetyStock) {
        return new StockListResponse(1L, "PRD-001", "밀가루", "식자재", quantity, safetyStock);
    }

    @Test
    @DisplayName("수량이 0이면 품절(OUT_OF_STOCK)로 판정하고 강조 대상이다")
    void status_quantityZero_outOfStock() {
        StockListResponse r = response(0, 10);

        assertThat(r.getStatus()).isEqualTo(StockStatus.OUT_OF_STOCK);
        assertThat(r.getStatusLabel()).isEqualTo("품절");
        assertThat(r.isShortage()).isTrue();
    }

    @Test
    @DisplayName("수량이 0보다 크고 안전재고 미만이면 미달(SHORTAGE)로 판정하고 강조 대상이다")
    void status_belowSafety_shortage() {
        StockListResponse r = response(5, 10);

        assertThat(r.getStatus()).isEqualTo(StockStatus.SHORTAGE);
        assertThat(r.getStatusLabel()).isEqualTo("미달");
        assertThat(r.isShortage()).isTrue();
    }

    @Test
    @DisplayName("수량이 안전재고와 같으면 정상(NORMAL)이다 (경계값: 미만이 아님)")
    void status_equalToSafety_normal() {
        StockListResponse r = response(10, 10);

        assertThat(r.getStatus()).isEqualTo(StockStatus.NORMAL);
        assertThat(r.isShortage()).isFalse();
    }

    @Test
    @DisplayName("수량이 안전재고를 초과하면 정상(NORMAL)이고 강조 대상이 아니다")
    void status_aboveSafety_normal() {
        StockListResponse r = response(15, 10);

        assertThat(r.getStatus()).isEqualTo(StockStatus.NORMAL);
        assertThat(r.getStatusLabel()).isEqualTo("정상");
        assertThat(r.isShortage()).isFalse();
    }
}
