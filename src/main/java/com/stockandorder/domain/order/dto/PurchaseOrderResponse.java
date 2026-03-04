package com.stockandorder.domain.order.dto;

import com.stockandorder.domain.order.entity.PurchaseOrder;
import com.stockandorder.domain.order.entity.PurchaseOrderItem;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class PurchaseOrderResponse {

    private final Long orderId;
    private final String orderNumber;
    private final String supplierName;
    private final String requesterName;
    private final String approverName;
    private final String status;
    private final String statusLabel;
    private final BigDecimal totalAmount;
    private final String note;
    private final LocalDateTime orderedAt;
    private final LocalDateTime approvedAt;
    private final List<ItemResponse> items;

    private PurchaseOrderResponse(PurchaseOrder order) {
        this.orderId = order.getOrderId();
        this.orderNumber = order.getOrderNumber();
        this.supplierName = order.getSupplier().getName();
        this.requesterName = order.getRequester().getName();
        this.approverName = order.getApprover() != null ? order.getApprover().getName() : null;
        this.status = order.getStatus().name();
        this.statusLabel = order.getStatus().getLabel();
        this.totalAmount = order.getTotalAmount();
        this.note = order.getNote();
        this.orderedAt = order.getOrderedAt();
        this.approvedAt = order.getApprovedAt();
        this.items = order.getItems().stream()
                .map(ItemResponse::new)
                .toList();
    }

    public static PurchaseOrderResponse from(PurchaseOrder order) {
        return new PurchaseOrderResponse(order);
    }

    @Getter
    public static class ItemResponse {

        private final Long orderItemId;
        private final String productName;
        private final String productCode;
        private final int quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal subtotal;
        private final int receivedQuantity;

        private ItemResponse(PurchaseOrderItem item) {
            this.orderItemId = item.getOrderItemId();
            this.productName = item.getProduct().getName();
            this.productCode = item.getProduct().getProductCode();
            this.quantity = item.getQuantity();
            this.unitPrice = item.getUnitPrice();
            this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            this.receivedQuantity = item.getReceivedQuantity();
        }
    }
}
