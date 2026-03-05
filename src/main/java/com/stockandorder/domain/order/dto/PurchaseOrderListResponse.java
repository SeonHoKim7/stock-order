package com.stockandorder.domain.order.dto;

import com.stockandorder.domain.order.enums.OrderStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class PurchaseOrderListResponse {

    private final Long orderId;
    private final String orderNumber;
    private final String supplierName;
    private final String requesterName;
    private final String status;
    private final String statusLabel;
    private final BigDecimal totalAmount;
    private final LocalDateTime orderedAt;

    public PurchaseOrderListResponse(Long orderId, String orderNumber, String supplierName,
                                     String requesterName, OrderStatus status,
                                     BigDecimal totalAmount, LocalDateTime orderedAt) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.supplierName = supplierName;
        this.requesterName = requesterName;
        this.status = status.name();
        this.statusLabel = status.getLabel();
        this.totalAmount = totalAmount;
        this.orderedAt = orderedAt;
    }
}
