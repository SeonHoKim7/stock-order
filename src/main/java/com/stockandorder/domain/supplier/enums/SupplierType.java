package com.stockandorder.domain.supplier.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SupplierType {
    PURCHASE("공급처"),
    SALES("판매처"),
    BOTH("공급처/판매처");

    private final String label;
}
