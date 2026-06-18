package com.stockandorder.domain.inbound.entity;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.order.entity.PurchaseOrderItem;
import com.stockandorder.domain.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class InboundItemTest {

    @Test
    @DisplayName("create() 호출 시 orderItem과 quantity가 설정되고 inbound는 아직 null이다")
    void create_setsOrderItemAndQuantity_inboundStillNull() {
        Category category = Category.create("식자재", null);
        Product product = Product.create("PRD-001", "밀가루", category, "KG",
                BigDecimal.valueOf(10000), 10, null);
        PurchaseOrderItem orderItem = PurchaseOrderItem.create(product, 50, BigDecimal.valueOf(10000));

        InboundItem item = InboundItem.create(orderItem, 30);

        assertThat(item.getOrderItem()).isEqualTo(orderItem);
        assertThat(item.getQuantity()).isEqualTo(30);
        // 입고 항목은 Inbound.create()를 통해서만 부모와 연결된다(단독 생성 시점에는 null).
        assertThat(item.getInbound()).isNull();
    }
}
