package com.stockandorder.domain.supplier.dto;

import com.stockandorder.domain.supplier.entity.Supplier;
import com.stockandorder.domain.supplier.enums.SupplierType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SupplierResponse {

    private final Long supplierId;
    private final String name;
    private final SupplierType supplierType;
    private final String supplierTypeLabel;
    private final String contactName;
    private final String contactPhone;
    private final String contactEmail;
    private final String address;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private SupplierResponse(Supplier supplier) {
        this.supplierId = supplier.getSupplierId();
        this.name = supplier.getName();
        this.supplierType = supplier.getSupplierType();
        this.supplierTypeLabel = supplier.getSupplierType().getLabel();
        this.contactName = supplier.getContactName();
        this.contactPhone = supplier.getContactPhone();
        this.contactEmail = supplier.getContactEmail();
        this.address = supplier.getAddress();
        this.active = supplier.isActive();
        this.createdAt = supplier.getCreatedAt();
        this.updatedAt = supplier.getUpdatedAt();
    }

    public static SupplierResponse from(Supplier supplier) {
        return new SupplierResponse(supplier);
    }
}
