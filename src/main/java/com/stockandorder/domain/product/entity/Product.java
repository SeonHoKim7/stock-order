package com.stockandorder.domain.product.entity;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Column(nullable = false, unique = true, length = 50)
    private String productCode;

    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int safetyStock;

    @Column(length = 500)
    private String description;

    // is 접두사 필드는 Hibernate가 컬럼명을 'active'로 잘못 매핑할 수 있어 명시적으로 지정
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    public static Product create(String productCode, String name, Category category,
                                 String unit, BigDecimal unitPrice, int safetyStock, String description) {
        Product product = new Product();
        product.productCode = productCode;
        product.name = name;
        product.category = category;
        product.unit = unit;
        product.unitPrice = unitPrice;
        product.safetyStock = safetyStock;
        product.description = description;
        product.isActive = true;
        return product;
    }

    public void update(String name, Category category, String unit, BigDecimal unitPrice,
                       int safetyStock, String description) {
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.unitPrice = unitPrice;
        this.safetyStock = safetyStock;
        this.description = description;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }
}
