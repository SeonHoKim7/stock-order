package com.stockandorder.domain.stock.entity;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.domain.stock.enums.StockChangeType;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockLogTest {

    private Product product;

    @BeforeEach
    void setUp() {
        Category category = Category.create("식자재", null);
        product = Product.create("PRD-001", "밀가루", category, "KG",
                BigDecimal.valueOf(10000), 10, null);
    }

    @Nested
    @DisplayName("of() 정상 생성")
    class CreateValid {

        @Test
        @DisplayName("INBOUND: 양수 changeQuantity로 정상 생성된다")
        void of_inbound_createsLog() {
            StockLog log = StockLog.of(product, StockChangeType.INBOUND,
                    10, 0, 10, 100L, null);

            assertThat(log.getChangeType()).isEqualTo(StockChangeType.INBOUND);
            assertThat(log.getChangeQuantity()).isEqualTo(10);
            assertThat(log.getBeforeQuantity()).isZero();
            assertThat(log.getAfterQuantity()).isEqualTo(10);
            assertThat(log.getReferenceId()).isEqualTo(100L);
            assertThat(log.getReason()).isNull();
        }

        @Test
        @DisplayName("OUTBOUND: 음수 changeQuantity로 정상 생성된다")
        void of_outbound_createsLog() {
            StockLog log = StockLog.of(product, StockChangeType.OUTBOUND,
                    -5, 10, 5, 200L, null);

            assertThat(log.getChangeType()).isEqualTo(StockChangeType.OUTBOUND);
            assertThat(log.getChangeQuantity()).isEqualTo(-5);
            assertThat(log.getAfterQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("ADJUST: 양수/음수 changeQuantity 모두 허용되며 referenceId는 null")
        void of_adjust_createsLog() {
            StockLog increase = StockLog.of(product, StockChangeType.ADJUST,
                    3, 10, 13, null, "재고 실사 가산");
            StockLog decrease = StockLog.of(product, StockChangeType.ADJUST,
                    -2, 13, 11, null, "파손 처리");

            assertThat(increase.getReferenceId()).isNull();
            assertThat(increase.getReason()).isEqualTo("재고 실사 가산");
            assertThat(decrease.getChangeQuantity()).isEqualTo(-2);
            assertThat(decrease.getAfterQuantity()).isEqualTo(11);
        }
    }

    @Nested
    @DisplayName("of() 방향성 검증")
    class DirectionValidation {

        @Test
        @DisplayName("INBOUND: 0 또는 음수 changeQuantity는 IllegalArgumentException")
        void of_inboundNonPositive_throwsException() {
            assertThatThrownBy(() -> StockLog.of(product, StockChangeType.INBOUND,
                    0, 10, 10, 1L, null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> StockLog.of(product, StockChangeType.INBOUND,
                    -1, 10, 9, 1L, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("OUTBOUND: 0 또는 양수 changeQuantity는 IllegalArgumentException")
        void of_outboundNonNegative_throwsException() {
            assertThatThrownBy(() -> StockLog.of(product, StockChangeType.OUTBOUND,
                    0, 10, 10, 1L, null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> StockLog.of(product, StockChangeType.OUTBOUND,
                    1, 10, 11, 1L, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("ADJUST: 0 changeQuantity는 IllegalArgumentException")
        void of_adjustZero_throwsException() {
            assertThatThrownBy(() -> StockLog.of(product, StockChangeType.ADJUST,
                    0, 10, 10, null, "사유"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("of() ADJUST 사유 불변식")
    class AdjustReasonValidation {

        @Test
        @DisplayName("ADJUST: reason이 null이면 STOCK_ADJUST_REASON_REQUIRED 예외")
        void of_adjustNullReason_throws() {
            assertThatThrownBy(() -> StockLog.of(product, StockChangeType.ADJUST,
                    3, 10, 13, null, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.STOCK_ADJUST_REASON_REQUIRED));
        }

        @Test
        @DisplayName("ADJUST: reason이 공백이면 STOCK_ADJUST_REASON_REQUIRED 예외")
        void of_adjustBlankReason_throws() {
            assertThatThrownBy(() -> StockLog.of(product, StockChangeType.ADJUST,
                    3, 10, 13, null, "   "))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.STOCK_ADJUST_REASON_REQUIRED));
        }

        @Test
        @DisplayName("INBOUND/OUTBOUND: reason이 null이어도 정상(자동 변동은 사유 불필요)")
        void of_autoChangeNullReason_ok() {
            StockLog inbound = StockLog.of(product, StockChangeType.INBOUND, 5, 0, 5, 1L, null);
            StockLog outbound = StockLog.of(product, StockChangeType.OUTBOUND, -2, 5, 3, 2L, null);

            assertThat(inbound.getReason()).isNull();
            assertThat(outbound.getReason()).isNull();
        }
    }

    @Nested
    @DisplayName("of() before+change=after 불변식")
    class InvariantValidation {

        @Test
        @DisplayName("after_quantity가 before_quantity + change_quantity와 일치하지 않으면 예외")
        void of_inconsistentInvariant_throwsException() {
            assertThatThrownBy(() -> StockLog.of(product, StockChangeType.INBOUND,
                    10, 0, 99, 1L, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
