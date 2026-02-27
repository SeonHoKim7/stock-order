package com.stockandorder.domain.product.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ProductCreateRequest {

    @NotBlank(message = "상품 코드를 입력해주세요.")
    @Size(max = 50, message = "상품 코드는 50자 이하여야 합니다.")
    private String productCode;

    @NotBlank(message = "상품명을 입력해주세요.")
    @Size(max = 200, message = "상품명은 200자 이하여야 합니다.")
    private String name;

    @NotNull(message = "카테고리를 선택해주세요.")
    private Long categoryId;

    @NotBlank(message = "단위를 입력해주세요.")
    @Size(max = 20, message = "단위는 20자 이하여야 합니다.")
    private String unit;

    @NotNull(message = "단가를 입력해주세요.")
    @DecimalMin(value = "0", message = "단가는 0 이상이어야 합니다.")
    private BigDecimal unitPrice;

    @Min(value = 0, message = "안전 재고량은 0 이상이어야 합니다.")
    private int safetyStock;

    @Size(max = 500, message = "설명은 500자 이하여야 합니다.")
    private String description;
}
