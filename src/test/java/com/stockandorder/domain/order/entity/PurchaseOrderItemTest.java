package com.stockandorder.domain.order.entity;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PurchaseOrderItemTest {

    private Product product;

    private PurchaseOrderItem newItem(int quantity) {
        if (product == null) {
            Category category = Category.create("식자재", null);
            product = Product.create("PRD-001", "밀가루", category, "KG",
                    BigDecimal.valueOf(10000), 10, null);
        }
        return PurchaseOrderItem.create(product, quantity, BigDecimal.valueOf(10000));
    }

    @Test
    @DisplayName("create() 호출 시 전달한 값으로 필드가 설정되고 receivedQuantity는 0이다")
    void create_setsAllFieldsAndReceivedQuantityZero() {
        Category category = Category.create("식자재", null);
        Product product = Product.create("PRD-001", "밀가루", category, "KG",
                BigDecimal.valueOf(10000), 10, null);

        PurchaseOrderItem item = PurchaseOrderItem.create(product, 50, BigDecimal.valueOf(10000));

        assertThat(item.getProduct()).isEqualTo(product);
        assertThat(item.getQuantity()).isEqualTo(50);
        assertThat(item.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(item.getReceivedQuantity()).isZero();
        assertThat(item.getPurchaseOrder()).isNull();
    }

    @Nested
    @DisplayName("receive (입고 수량 누적 / 초과 입고 금지 불변식)")
    class Receive {

        @Test
        @DisplayName("발주 수량 이내로 입고하면 receivedQuantity가 누적된다")
        void receive_withinOrderedQuantity_accumulates() {
            PurchaseOrderItem item = newItem(10);

            item.receive(4);
            item.receive(6);

            assertThat(item.getReceivedQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("발주 수량만큼 정확히 입고하면 receivedQuantity가 발주 수량과 같아진다")
        void receive_exactOrderedQuantity_fullyReceived() {
            PurchaseOrderItem item = newItem(10);

            item.receive(10);

            assertThat(item.getReceivedQuantity()).isEqualTo(item.getQuantity());
        }

        @Test
        @DisplayName("누적 입고량이 발주 수량을 초과하면 INBOUND_QUANTITY_EXCEEDED 예외가 발생한다")
        void receive_exceedsOrderedQuantity_throwsException() {
            PurchaseOrderItem item = newItem(10);
            item.receive(8);

            assertThatThrownBy(() -> item.receive(3))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INBOUND_QUANTITY_EXCEEDED));
        }

        @Test
        @DisplayName("초과로 실패한 경우 receivedQuantity는 변경되지 않는다")
        void receive_exceeds_leavesReceivedQuantityUnchanged() {
            PurchaseOrderItem item = newItem(10);
            item.receive(8);

            assertThatThrownBy(() -> item.receive(3)).isInstanceOf(BusinessException.class);

            assertThat(item.getReceivedQuantity()).isEqualTo(8);
        }
    }
}
