package com.stockandorder.domain.inbound.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class InboundSearchCondition {

    // 입고 목록 거래처 검색(FR-06-03). supplier는 inbound에 중복 저장하지 않고 발주를 거쳐 조회한다(B-1).
    private Long supplierId;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
