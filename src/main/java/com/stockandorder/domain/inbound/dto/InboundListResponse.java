package com.stockandorder.domain.inbound.dto;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class InboundListResponse {

    private final Long inboundId;
    private final String inboundNumber;
    private final String orderNumber;
    private final String supplierName;
    private final String processorName;
    private final LocalDate inboundDate;

    public InboundListResponse(Long inboundId, String inboundNumber, String orderNumber,
                               String supplierName, String processorName, LocalDate inboundDate) {
        this.inboundId = inboundId;
        this.inboundNumber = inboundNumber;
        this.orderNumber = orderNumber;
        this.supplierName = supplierName;
        this.processorName = processorName;
        this.inboundDate = inboundDate;
    }
}
