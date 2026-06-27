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

    // 매입가: 공급처에서 사들이는 단가. 발주 시 PurchaseOrderItem이 스냅샷한다.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    // 매출가: 판매처에 파는 단가. 출고 시 OutboundItem이 스냅샷한다.
    // 매입/매출을 분리해 출고 금액(매출)이 매입 원가와 구분되도록 한다(마진 0 모델 회피).
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(nullable = false)
    private int safetyStock;

    @Column(length = 500)
    private String description;

    // is 접두사 필드는 Hibernate가 컬럼명을 'active'로 잘못 매핑할 수 있어 명시적으로 지정
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    public static Product create(String productCode, String name, Category category, String unit,
                                 BigDecimal purchasePrice, BigDecimal salePrice,
                                 int safetyStock, String description) {
        Product product = new Product();
        product.productCode = productCode;
        product.name = name;
        product.category = category;
        product.unit = unit;
        product.purchasePrice = purchasePrice;
        product.salePrice = salePrice;
        product.safetyStock = safetyStock;
        product.description = description;
        product.isActive = true;
        return product;
    }

    // 매출가 미지정 시 매입가와 동일하게 출발(신규 상품은 원가로 시작, 이후 매출가 책정). 단순 생성/테스트 편의.
    public static Product create(String productCode, String name, Category category, String unit,
                                 BigDecimal purchasePrice, int safetyStock, String description) {
        return create(productCode, name, category, unit, purchasePrice, purchasePrice, safetyStock, description);
    }

    public void update(String name, Category category, String unit,
                       BigDecimal purchasePrice, BigDecimal salePrice,
                       int safetyStock, String description) {
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.purchasePrice = purchasePrice;
        this.salePrice = salePrice;
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
