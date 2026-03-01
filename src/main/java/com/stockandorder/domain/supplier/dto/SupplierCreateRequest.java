package com.stockandorder.domain.supplier.dto;

import com.stockandorder.domain.supplier.enums.SupplierType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SupplierCreateRequest {

    @NotBlank(message = "거래처명을 입력해주세요.")
    @Size(max = 200, message = "거래처명은 200자 이하여야 합니다.")
    private String name;

    @NotNull(message = "거래처 유형을 선택해주세요.")
    private SupplierType supplierType;

    @Size(max = 50, message = "담당자명은 50자 이하여야 합니다.")
    private String contactName;

    @Size(max = 20, message = "연락처는 20자 이하여야 합니다.")
    private String contactPhone;

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
    private String contactEmail;

    @Size(max = 500, message = "주소는 500자 이하여야 합니다.")
    private String address;
}
