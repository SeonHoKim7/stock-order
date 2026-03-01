package com.stockandorder.domain.product.entity;

import com.stockandorder.domain.category.entity.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    // create

    @Test
    @DisplayName("create() 호출 시 전달한 값으로 필드가 설정되고 isActive는 true이다")
    void create_setsAllFieldsAndIsActiveTrue() {
        Category category = Category.create("전자기기", null);

        Product product = Product.create(
                "PRD-00001", "노트북", category, "EA",
                BigDecimal.valueOf(1200000), 3, "업무용 노트북"
        );

        assertThat(product.getProductCode()).isEqualTo("PRD-00001");
        assertThat(product.getName()).isEqualTo("노트북");
        assertThat(product.getCategory()).isEqualTo(category);
        assertThat(product.getUnit()).isEqualTo("EA");
        assertThat(product.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(1200000));
        assertThat(product.getSafetyStock()).isEqualTo(3);
        assertThat(product.getDescription()).isEqualTo("업무용 노트북");
        assertThat(product.isActive()).isTrue();
    }


    // update

    @Test
    @DisplayName("update() 호출 시 수정 가능한 필드들이 변경된다")
    void update_changesEditableFields() {
        Category oldCategory = Category.create("전자기기", null);
        Category newCategory = Category.create("사무용품", null);
        Product product = Product.create("PRD-00001", "노트북", oldCategory, "EA",
                BigDecimal.valueOf(1200000), 3, "구설명");

        product.update("노트북 Pro", newCategory, "BOX", BigDecimal.valueOf(1500000), 5, "신설명");

        assertThat(product.getName()).isEqualTo("노트북 Pro");
        assertThat(product.getCategory()).isEqualTo(newCategory);
        assertThat(product.getUnit()).isEqualTo("BOX");
        assertThat(product.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(1500000));
        assertThat(product.getSafetyStock()).isEqualTo(5);
        assertThat(product.getDescription()).isEqualTo("신설명");
    }

    // deactivate / activate

    @Test
    @DisplayName("deactivate() 호출 시 isActive가 false가 된다")
    void deactivate_setsIsActiveFalse() {
        Category category = Category.create("미분류", null);
        Product product = Product.create("PRD-00001", "노트북", category, "EA",
                BigDecimal.valueOf(1000000), 0, null);

        product.deactivate();

        assertThat(product.isActive()).isFalse();
    }

    @Test
    @DisplayName("activate() 호출 시 isActive가 true가 된다")
    void activate_setsIsActiveTrue() {
        Category category = Category.create("미분류", null);
        Product product = Product.create("PRD-00001", "노트북", category, "EA",
                BigDecimal.valueOf(1000000), 0, null);
        product.deactivate();

        product.activate();

        assertThat(product.isActive()).isTrue();
    }
}
