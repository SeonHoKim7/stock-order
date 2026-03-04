package com.stockandorder.domain.order.entity;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PurchaseOrderItemTest {

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
}
