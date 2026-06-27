package com.stockandorder.domain.outbound.dto;

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
public class OutboundCreateRequest {

    @NotNull(message = "판매처를 선택해주세요.")
    private Long supplierId;

    // F-2: 실제 출고일. created_at(시스템 시각)과 별개로 사람이 입력하며 과거 소급도 가능하다.
    @NotNull(message = "출고일을 입력해주세요.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate outboundDate;

    @Size(max = 500, message = "비고는 500자 이하여야 합니다.")
    private String note;

    @NotEmpty(message = "출고 항목을 1개 이상 추가해주세요.")
    @Valid
    private List<ItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ItemRequest {

        // A-3: 발주 항목이 아니라 상품을 직접 가리킨다(출고는 상위 문서가 없음).
        @NotNull(message = "상품을 선택해주세요.")
        private Long productId;

        @Min(value = 1, message = "출고 수량은 1 이상이어야 합니다.")
        private int quantity;
    }
}
