package com.stockandorder.domain.outbound.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class OutboundListResponse {

    private final Long outboundId;
    private final String outboundNumber;
    private final String supplierName;
    private final String processorName;
    private final BigDecimal totalAmount;
    private final LocalDate outboundDate;

    public OutboundListResponse(Long outboundId, String outboundNumber, String supplierName,
                                String processorName, BigDecimal totalAmount, LocalDate outboundDate) {
        this.outboundId = outboundId;
        this.outboundNumber = outboundNumber;
        this.supplierName = supplierName;
        this.processorName = processorName;
        this.totalAmount = totalAmount;
        this.outboundDate = outboundDate;
    }
}
