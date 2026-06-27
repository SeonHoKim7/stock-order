package com.stockandorder.domain.outbound.entity;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.domain.supplier.entity.Supplier;
import com.stockandorder.domain.supplier.enums.SupplierType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OutboundTest {

    private Product product(String code, BigDecimal salePrice) {
        Category category = Category.create("식자재", null);
        return Product.create(code, "상품", category, "KG",
                BigDecimal.valueOf(10000), salePrice, 10, null);
    }

    @Test
    @DisplayName("create() 호출 시 필드가 설정되고 항목이 양방향으로 연결된다")
    void create_setsFieldsAndWiresItems() {
        Supplier supplier = Supplier.create("판매처A", SupplierType.SALES,
                "담당", "010-0000-0000", "a@a.com", "서울");
        Member processor = Member.create("manager1", "pw", "매니저", "m@test.com", Role.MANAGER);

        OutboundItem item1 = OutboundItem.create(product("PRD-001", BigDecimal.valueOf(13000)), 5, BigDecimal.valueOf(13000));
        OutboundItem item2 = OutboundItem.create(product("PRD-002", BigDecimal.valueOf(7000)), 2, BigDecimal.valueOf(7000));

        Outbound outbound = Outbound.create("OUT-20260622-001", supplier, processor,
                LocalDate.of(2026, 6, 22), "비고", List.of(item1, item2));

        assertThat(outbound.getOutboundNumber()).isEqualTo("OUT-20260622-001");
        assertThat(outbound.getSupplier()).isEqualTo(supplier);
        assertThat(outbound.getProcessor()).isEqualTo(processor);
        assertThat(outbound.getOutboundDate()).isEqualTo(LocalDate.of(2026, 6, 22));
        assertThat(outbound.getNote()).isEqualTo("비고");
        assertThat(outbound.getItems()).containsExactly(item1, item2);
        // 양방향 연결: 각 항목이 부모 출고를 가리킨다
        assertThat(item1.getOutbound()).isEqualTo(outbound);
        assertThat(item2.getOutbound()).isEqualTo(outbound);
    }

    @Test
    @DisplayName("create() 호출 시 totalAmount는 항목들의 (단가 × 수량) 합으로 자동 계산된다")
    void create_calculatesTotalAmount() {
        Supplier supplier = Supplier.create("판매처A", SupplierType.BOTH,
                "담당", "010-0000-0000", "a@a.com", "서울");
        Member processor = Member.create("manager1", "pw", "매니저", "m@test.com", Role.MANAGER);

        OutboundItem item1 = OutboundItem.create(product("PRD-001", BigDecimal.valueOf(13000)), 5, BigDecimal.valueOf(13000));
        OutboundItem item2 = OutboundItem.create(product("PRD-002", BigDecimal.valueOf(7000)), 2, BigDecimal.valueOf(7000));

        Outbound outbound = Outbound.create("OUT-20260622-001", supplier, processor,
                LocalDate.now(), null, List.of(item1, item2));

        // 13,000 * 5 + 7,000 * 2 = 79,000
        assertThat(outbound.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(79000));
    }
}
