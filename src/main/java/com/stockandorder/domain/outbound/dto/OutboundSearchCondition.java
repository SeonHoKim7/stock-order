package com.stockandorder.domain.outbound.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class OutboundSearchCondition {

    // 출고 목록 거래처 검색(FR-07-04). 출고는 supplier를 직접 참조하므로 발주를 거치지 않는다.
    private Long supplierId;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
