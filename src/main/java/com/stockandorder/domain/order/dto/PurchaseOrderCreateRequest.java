package com.stockandorder.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrderCreateRequest {

    @NotNull(message = "거래처를 선택해주세요.")
    private Long supplierId;

    @Size(max = 500, message = "비고는 500자 이하여야 합니다.")
    private String note;

    @NotEmpty(message = "발주 항목을 1개 이상 추가해주세요.")
    @Valid
    private List<ItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ItemRequest {

        @NotNull(message = "상품을 선택해주세요.")
        private Long productId;

        @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
        private int quantity;
    }
}
