package com.stockandorder.domain.inbound.entity;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import com.stockandorder.domain.order.entity.PurchaseOrder;
import com.stockandorder.domain.order.entity.PurchaseOrderItem;
import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.domain.supplier.entity.Supplier;
import com.stockandorder.domain.supplier.enums.SupplierType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InboundTest {

    private PurchaseOrder order;
    private Member processor;
    private PurchaseOrderItem orderItem1;
    private PurchaseOrderItem orderItem2;

    @BeforeEach
    void setUp() {
        Supplier supplier = Supplier.create("공급처A", SupplierType.PURCHASE,
                "담당자", "010-1234-5678", "test@test.com", "서울");
        Member requester = Member.create("staff1", "password", "직원1", "staff1@test.com", Role.STAFF);
        processor = Member.create("manager1", "password", "매니저1", "manager1@test.com", Role.MANAGER);

        Category category = Category.create("식자재", null);
        Product product1 = Product.create("PRD-001", "밀가루", category, "KG",
                BigDecimal.valueOf(10000), 10, null);
        Product product2 = Product.create("PRD-002", "설탕", category, "KG",
                BigDecimal.valueOf(5000), 5, null);

        order = PurchaseOrder.create("PO-20260610-001", supplier, requester, null);
        orderItem1 = PurchaseOrderItem.create(product1, 10, BigDecimal.valueOf(10000));
        orderItem2 = PurchaseOrderItem.create(product2, 20, BigDecimal.valueOf(5000));
        order.addItem(orderItem1);
        order.addItem(orderItem2);
    }

    @Test
    @DisplayName("create() 호출 시 전달한 값으로 필드가 설정된다")
    void create_setsAllFields() {
        LocalDate inboundDate = LocalDate.of(2026, 6, 10);
        List<InboundItem> items = List.of(
                InboundItem.create(orderItem1, 10),
                InboundItem.create(orderItem2, 20));

        Inbound inbound = Inbound.create("IN-20260610-001", order, processor,
                inboundDate, "정상 입고", items);

        assertThat(inbound.getInboundNumber()).isEqualTo("IN-20260610-001");
        assertThat(inbound.getPurchaseOrder()).isEqualTo(order);
        assertThat(inbound.getProcessor()).isEqualTo(processor);
        assertThat(inbound.getInboundDate()).isEqualTo(inboundDate);
        assertThat(inbound.getNote()).isEqualTo("정상 입고");
        assertThat(inbound.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("create() 호출 시 각 항목의 inbound가 자신을 가리키도록 양방향으로 연결된다")
    void create_wiresItemsBidirectionally() {
        List<InboundItem> items = List.of(
                InboundItem.create(orderItem1, 10),
                InboundItem.create(orderItem2, 20));

        Inbound inbound = Inbound.create("IN-20260610-001", order, processor,
                LocalDate.now(), null, items);

        assertThat(inbound.getItems())
                .allSatisfy(item -> assertThat(item.getInbound()).isSameAs(inbound));
    }
}
