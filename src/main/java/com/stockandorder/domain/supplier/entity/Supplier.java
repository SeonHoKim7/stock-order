package com.stockandorder.domain.supplier.entity;

import com.stockandorder.domain.supplier.enums.SupplierType;
import com.stockandorder.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "supplier")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Supplier extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long supplierId;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupplierType supplierType;

    @Column(length = 50)
    private String contactName;

    @Column(length = 20)
    private String contactPhone;

    @Column(length = 100)
    private String contactEmail;

    @Column(length = 500)
    private String address;

    // is 접두사 필드는 Hibernate가 컬럼명을 'active'로 잘못 매핑할 수 있어 명시적으로 지정
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    public static Supplier create(String name, SupplierType supplierType, String contactName,
                                  String contactPhone, String contactEmail, String address) {
        Supplier supplier = new Supplier();
        supplier.name = name;
        supplier.supplierType = supplierType;
        supplier.contactName = contactName;
        supplier.contactPhone = contactPhone;
        supplier.contactEmail = contactEmail;
        supplier.address = address;
        supplier.isActive = true;
        return supplier;
    }

    public void update(String name, SupplierType supplierType, String contactName,
                       String contactPhone, String contactEmail, String address) {
        this.name = name;
        this.supplierType = supplierType;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.address = address;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }
}
