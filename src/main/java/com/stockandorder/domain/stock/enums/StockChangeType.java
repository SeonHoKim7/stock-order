package com.stockandorder.domain.stock.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StockChangeType {
    INBOUND("입고"),
    OUTBOUND("출고"),
    ADJUST("수동조정");

    private final String label;
}
