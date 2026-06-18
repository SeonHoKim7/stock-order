package com.stockandorder.domain.inbound.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class InboundCreateRequest {

    @NotNull(message = "발주를 선택해주세요.")
    private Long orderId;

    // F-2: 실제 입고일. created_at(시스템 시각)과 별개로 사람이 입력하며 과거 소급도 가능하다.
    @NotNull(message = "입고일을 입력해주세요.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate inboundDate;

    @Size(max = 500, message = "비고는 500자 이하여야 합니다.")
    private String note;

    @NotEmpty(message = "입고 항목을 1개 이상 추가해주세요.")
    @Valid
    private List<ItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ItemRequest {

        // A-3: 어느 발주 항목에 대한 입고인지. product가 아니라 발주 항목(줄)을 가리킨다.
        @NotNull(message = "발주 항목을 선택해주세요.")
        private Long orderItemId;

        @Min(value = 1, message = "입고 수량은 1 이상이어야 합니다.")
        private int quantity;
    }
}
