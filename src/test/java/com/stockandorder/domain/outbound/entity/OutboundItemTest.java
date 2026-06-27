package com.stockandorder.domain.outbound.entity;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OutboundItemTest {

    @Test
    @DisplayName("create() 호출 시 상품/수량/단가(매출가 스냅샷)가 설정된다")
    void create_setsFields() {
        Category category = Category.create("식자재", null);
        Product product = Product.create("PRD-001", "밀가루", category, "KG",
                BigDecimal.valueOf(10000), BigDecimal.valueOf(13000), 10, null);

        OutboundItem item = OutboundItem.create(product, 5, BigDecimal.valueOf(13000));

        assertThat(item.getProduct()).isEqualTo(product);
        assertThat(item.getQuantity()).isEqualTo(5);
        assertThat(item.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(13000));
    }
}
