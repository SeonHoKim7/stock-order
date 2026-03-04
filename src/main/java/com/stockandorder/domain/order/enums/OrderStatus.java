package com.stockandorder.domain.order.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    PENDING("대기"),
    APPROVED("승인"),
    REJECTED("반려"),
    CANCELLED("취소"),
    IN_PROGRESS("진행중"),
    COMPLETED("완료");

    private final String label;
}
