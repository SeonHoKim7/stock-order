package com.stockandorder.domain.product.dto;

import com.stockandorder.domain.product.entity.Product;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class ProductResponse {

    private final Long productId;
    private final String productCode;
    private final String name;
    private final Long categoryId;
    private final String categoryName;
    private final String unit;
    private final BigDecimal unitPrice;
    private final int safetyStock;
    private final String description;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private ProductResponse(Product product) {
        this.productId = product.getProductId();
        this.productCode = product.getProductCode();
        this.name = product.getName();
        this.categoryId = product.getCategory().getCategoryId();
        this.categoryName = product.getCategory().getName();
        this.unit = product.getUnit();
        this.unitPrice = product.getUnitPrice();
        this.safetyStock = product.getSafetyStock();
        this.description = product.getDescription();
        this.active = product.isActive();
        this.createdAt = product.getCreatedAt();
        this.updatedAt = product.getUpdatedAt();
    }

    public static ProductResponse from(Product product) {
        return new ProductResponse(product);
    }
}
