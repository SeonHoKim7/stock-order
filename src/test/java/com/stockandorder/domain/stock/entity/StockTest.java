package com.stockandorder.domain.stock.entity;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTest {

    private Product product;

    @BeforeEach
    void setUp() {
        Category category = Category.create("식자재", null);
        product = Product.create("PRD-001", "밀가루", category, "KG",
                BigDecimal.valueOf(10000), 10, null);
    }

    @Test
    @DisplayName("create() 호출 시 quantity=0 으로 초기화된다")
    void create_initializesQuantityToZero() {
        Stock stock = Stock.create(product);

        assertThat(stock.getProduct()).isEqualTo(product);
        assertThat(stock.getQuantity()).isZero();
    }

    @Nested
    @DisplayName("increase")
    class Increase {

        @Test
        @DisplayName("양수 입력 시 수량이 증가한다")
        void increase_positiveAmount_increasesQuantity() {
            Stock stock = Stock.create(product);

            stock.increase(50);
            stock.increase(30);

            assertThat(stock.getQuantity()).isEqualTo(80);
        }

        @Test
        @DisplayName("0 또는 음수 입력 시 IllegalArgumentException이 발생한다")
        void increase_nonPositiveAmount_throwsException() {
            Stock stock = Stock.create(product);

            assertThatThrownBy(() -> stock.increase(0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> stock.increase(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("decrease")
    class Decrease {

        @Test
        @DisplayName("재고 범위 내 차감 시 수량이 감소한다")
        void decrease_withinStock_decreasesQuantity() {
            Stock stock = Stock.create(product);
            stock.increase(100);

            stock.decrease(30);

            assertThat(stock.getQuantity()).isEqualTo(70);
        }

        @Test
        @DisplayName("보유 재고를 초과한 차감 시 STOCK_INSUFFICIENT 예외가 발생한다")
        void decrease_exceedingStock_throwsException() {
            Stock stock = Stock.create(product);
            stock.increase(10);

            assertThatThrownBy(() -> stock.decrease(11))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.STOCK_INSUFFICIENT));
        }

        @Test
        @DisplayName("0 또는 음수 차감 시 IllegalArgumentException이 발생한다")
        void decrease_nonPositiveAmount_throwsException() {
            Stock stock = Stock.create(product);
            stock.increase(10);

            assertThatThrownBy(() -> stock.decrease(0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> stock.decrease(-5))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("adjust")
    class Adjust {

        @Test
        @DisplayName("양수 delta로 조정 시 수량이 증가한다")
        void adjust_positiveDelta_increasesQuantity() {
            Stock stock = Stock.create(product);
            stock.increase(10);

            stock.adjust(5);

            assertThat(stock.getQuantity()).isEqualTo(15);
        }

        @Test
        @DisplayName("음수 delta로 조정 시 수량이 감소한다")
        void adjust_negativeDelta_decreasesQuantity() {
            Stock stock = Stock.create(product);
            stock.increase(10);

            stock.adjust(-3);

            assertThat(stock.getQuantity()).isEqualTo(7);
        }

        @Test
        @DisplayName("delta 0 입력 시 IllegalArgumentException이 발생한다")
        void adjust_zeroDelta_throwsException() {
            Stock stock = Stock.create(product);

            assertThatThrownBy(() -> stock.adjust(0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("음수 delta로 결과가 음수가 되면 STOCK_INSUFFICIENT 예외가 발생한다")
        void adjust_resultBelowZero_throwsException() {
            Stock stock = Stock.create(product);
            stock.increase(5);

            assertThatThrownBy(() -> stock.adjust(-10))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.STOCK_INSUFFICIENT));
        }
    }
}
