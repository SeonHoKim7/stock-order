package com.stockandorder.domain.inbound.dto;

import com.stockandorder.domain.inbound.entity.Inbound;
import com.stockandorder.domain.inbound.entity.InboundItem;
import com.stockandorder.domain.order.entity.PurchaseOrderItem;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class InboundResponse {

    private final Long inboundId;
    private final String inboundNumber;
    private final Long orderId;
    private final String orderNumber;
    private final String supplierName;
    private final String processorName;
    private final LocalDate inboundDate;
    private final LocalDateTime createdAt;
    private final String note;
    private final List<ItemResponse> items;

    private InboundResponse(Inbound inbound) {
        this.inboundId = inbound.getInboundId();
        this.inboundNumber = inbound.getInboundNumber();
        this.orderId = inbound.getPurchaseOrder().getOrderId();
        this.orderNumber = inbound.getPurchaseOrder().getOrderNumber();
        this.supplierName = inbound.getPurchaseOrder().getSupplier().getName();
        this.processorName = inbound.getProcessor().getName();
        this.inboundDate = inbound.getInboundDate();
        this.createdAt = inbound.getCreatedAt();
        this.note = inbound.getNote();
        this.items = inbound.getItems().stream()
                .map(ItemResponse::new)
                .toList();
    }

    public static InboundResponse from(Inbound inbound) {
        return new InboundResponse(inbound);
    }

    @Getter
    public static class ItemResponse {

        private final Long inboundItemId;
        private final Long orderItemId;
        private final String productName;
        private final String productCode;
        private final int quantity;
        private final int orderedQuantity;
        private final int receivedQuantity;

        private ItemResponse(InboundItem item) {
            PurchaseOrderItem orderItem = item.getOrderItem();
            this.inboundItemId = item.getInboundItemId();
            this.orderItemId = orderItem.getOrderItemId();
            this.productName = orderItem.getProduct().getName();
            this.productCode = orderItem.getProduct().getProductCode();
            this.quantity = item.getQuantity();
            this.orderedQuantity = orderItem.getQuantity();
            this.receivedQuantity = orderItem.getReceivedQuantity();
        }
    }
}
