package com.stockandorder.domain.stock.dto;

import com.stockandorder.domain.stock.enums.StockChangeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class StockLogResponseTest {

    private StockLogResponse response(StockChangeType changeType, int changeQuantity, Long referenceId, String reason) {
        return new StockLogResponse(LocalDateTime.now(), 1L, "PRD-001", "밀가루",
                changeType, changeQuantity, 10, 10 + changeQuantity, referenceId, reason, "매니저1");
    }

    @Nested
    @DisplayName("변동량 표시")
    class ChangeQuantityLabel {

        @Test
        @DisplayName("증가는 부호를 붙여 보여준다")
        void increase_hasPlusSign() {
            StockLogResponse log = response(StockChangeType.INBOUND, 10, 1L, null);

            assertThat(log.getChangeQuantityLabel()).isEqualTo("+10");
            assertThat(log.isIncrease()).isTrue();
        }

        @Test
        @DisplayName("감소는 음수 부호를 그대로 쓴다")
        void decrease_keepsMinusSign() {
            StockLogResponse log = response(StockChangeType.OUTBOUND, -3, 2L, null);

            assertThat(log.getChangeQuantityLabel()).isEqualTo("-3");
            assertThat(log.isIncrease()).isFalse();
        }
    }

    @Nested
    @DisplayName("원본 문서 링크")
    class ReferenceUrl {

        @Test
        @DisplayName("입고 이력은 입고 상세로 연결된다")
        void inbound_linksToInboundDetail() {
            assertThat(response(StockChangeType.INBOUND, 10, 100L, null).getReferenceUrl())
                    .isEqualTo("/inbounds/100");
        }

        @Test
        @DisplayName("출고 이력은 출고 상세로 연결된다")
        void outbound_linksToOutboundDetail() {
            assertThat(response(StockChangeType.OUTBOUND, -5, 200L, null).getReferenceUrl())
                    .isEqualTo("/outbounds/200");
        }

        @Test
        @DisplayName("수동 조정은 원본 문서가 없으므로 링크가 없다")
        void adjust_hasNoLink() {
            // referenceId가 없는 대신 reason이 유일한 추적 단서다(I-4)
            StockLogResponse log = response(StockChangeType.ADJUST, -2, null, "파손 처리");

            assertThat(log.getReferenceUrl()).isNull();
            assertThat(log.getReason()).isEqualTo("파손 처리");
        }
    }

    @Test
    @DisplayName("변동 유형은 화면 표시용 라벨로 변환된다")
    void changeTypeLabel() {
        assertThat(response(StockChangeType.INBOUND, 1, 1L, null).getChangeTypeLabel()).isEqualTo("입고");
        assertThat(response(StockChangeType.OUTBOUND, -1, 1L, null).getChangeTypeLabel()).isEqualTo("출고");
        assertThat(response(StockChangeType.ADJUST, 1, null, "사유").getChangeTypeLabel()).isEqualTo("수동조정");
    }
}
