package com.stockandorder.domain.outbound.dto;

import com.stockandorder.domain.outbound.entity.Outbound;
import com.stockandorder.domain.outbound.entity.OutboundItem;
import com.stockandorder.domain.product.entity.Product;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OutboundResponse {

    private final Long outboundId;
    private final String outboundNumber;
    private final Long supplierId;
    private final String supplierName;
    private final String processorName;
    private final BigDecimal totalAmount;
    private final LocalDate outboundDate;
    private final LocalDateTime createdAt;
    private final String note;
    private final List<ItemResponse> items;

    private OutboundResponse(Outbound outbound) {
        this.outboundId = outbound.getOutboundId();
        this.outboundNumber = outbound.getOutboundNumber();
        this.supplierId = outbound.getSupplier().getSupplierId();
        this.supplierName = outbound.getSupplier().getName();
        this.processorName = outbound.getProcessor().getName();
        this.totalAmount = outbound.getTotalAmount();
        this.outboundDate = outbound.getOutboundDate();
        this.createdAt = outbound.getCreatedAt();
        this.note = outbound.getNote();
        this.items = outbound.getItems().stream()
                .map(ItemResponse::new)
                .toList();
    }

    public static OutboundResponse from(Outbound outbound) {
        return new OutboundResponse(outbound);
    }

    @Getter
    public static class ItemResponse {

        private final Long outboundItemId;
        private final String productName;
        private final String productCode;
        private final int quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal lineAmount;

        private ItemResponse(OutboundItem item) {
            Product product = item.getProduct();
            this.outboundItemId = item.getOutboundItemId();
            this.productName = product.getName();
            this.productCode = product.getProductCode();
            this.quantity = item.getQuantity();
            this.unitPrice = item.getUnitPrice();
            this.lineAmount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        }
    }
}
