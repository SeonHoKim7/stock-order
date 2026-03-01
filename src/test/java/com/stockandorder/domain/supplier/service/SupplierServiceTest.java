package com.stockandorder.domain.supplier.service;

import com.stockandorder.domain.supplier.dto.SupplierCreateRequest;
import com.stockandorder.domain.supplier.dto.SupplierResponse;
import com.stockandorder.domain.supplier.dto.SupplierUpdateRequest;
import com.stockandorder.domain.supplier.entity.Supplier;
import com.stockandorder.domain.supplier.enums.SupplierType;
import com.stockandorder.domain.supplier.repository.SupplierRepository;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @InjectMocks
    private SupplierService supplierService;

    @Mock
    private SupplierRepository supplierRepository;

    // searchSuppliers

    @Test
    @DisplayName("빈 문자열 키워드는 null로 변환하여 repository에 전달한다")
    void searchSuppliers_blankKeyword_passesNullToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        given(supplierRepository.search(null, null, pageable)).willReturn(Page.empty());

        supplierService.searchSuppliers("   ", null, pageable);

        then(supplierRepository).should().search(null, null, pageable);
    }

    @Test
    @DisplayName("null 키워드는 그대로 null로 repository에 전달한다")
    void searchSuppliers_nullKeyword_passesNullToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        given(supplierRepository.search(null, null, pageable)).willReturn(Page.empty());

        supplierService.searchSuppliers(null, null, pageable);

        then(supplierRepository).should().search(null, null, pageable);
    }

    @Test
    @DisplayName("검색 결과를 SupplierResponse 페이지로 변환하여 반환한다")
    void searchSuppliers_returnsPageOfSupplierResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        Supplier supplier = Supplier.create("(주)테스트", SupplierType.PURCHASE, "홍길동", "010-1234-5678", null, null);
        given(supplierRepository.search(null, null, pageable))
                .willReturn(new PageImpl<>(List.of(supplier)));

        Page<SupplierResponse> result = supplierService.searchSuppliers(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("(주)테스트");
    }

    // getSupplier

    @Test
    @DisplayName("존재하는 거래처 조회 시 SupplierResponse를 반환한다")
    void getSupplier_existingSupplier_returnsResponse() {
        Supplier supplier = Supplier.create("(주)테스트", SupplierType.BOTH, "홍길동", null, null, null);
        given(supplierRepository.findById(1L)).willReturn(Optional.of(supplier));

        SupplierResponse result = supplierService.getSupplier(1L);

        assertThat(result.getName()).isEqualTo("(주)테스트");
        assertThat(result.getSupplierType()).isEqualTo(SupplierType.BOTH);
        assertThat(result.getSupplierTypeLabel()).isEqualTo("공급처/판매처");
    }

    @Test
    @DisplayName("존재하지 않는 거래처 조회 시 SUPPLIER_NOT_FOUND 예외가 발생한다")
    void getSupplier_notFound_throwsException() {
        given(supplierRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.getSupplier(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SUPPLIER_NOT_FOUND));
    }

    // createSupplier

    @Test
    @DisplayName("거래처를 정상 등록하면 저장된다")
    void createSupplier_validRequest_savesSupplier() {
        SupplierCreateRequest request = createRequest("(주)테스트", SupplierType.PURCHASE);

        supplierService.createSupplier(request);

        then(supplierRepository).should().save(any(Supplier.class));
    }

    // updateSupplier

    @Test
    @DisplayName("거래처 수정 시 전달한 값으로 필드가 변경된다")
    void updateSupplier_validRequest_updatesFields() {
        Supplier supplier = Supplier.create("구거래처", SupplierType.PURCHASE, null, null, null, null);
        given(supplierRepository.findById(1L)).willReturn(Optional.of(supplier));

        SupplierUpdateRequest request = updateRequest("신거래처", SupplierType.BOTH, "김철수", "02-1234-5678");
        supplierService.updateSupplier(1L, request);

        assertThat(supplier.getName()).isEqualTo("신거래처");
        assertThat(supplier.getSupplierType()).isEqualTo(SupplierType.BOTH);
        assertThat(supplier.getContactName()).isEqualTo("김철수");
        assertThat(supplier.getContactPhone()).isEqualTo("02-1234-5678");
    }

    @Test
    @DisplayName("존재하지 않는 거래처 수정 시 SUPPLIER_NOT_FOUND 예외가 발생한다")
    void updateSupplier_notFound_throwsException() {
        given(supplierRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.updateSupplier(999L, new SupplierUpdateRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SUPPLIER_NOT_FOUND));
    }

    // deactivateSupplier

    @Test
    @DisplayName("deactivateSupplier() 호출 시 isActive가 false가 된다")
    void deactivateSupplier_setsIsActiveFalse() {
        Supplier supplier = Supplier.create("(주)테스트", SupplierType.PURCHASE, null, null, null, null);
        given(supplierRepository.findById(1L)).willReturn(Optional.of(supplier));

        supplierService.deactivateSupplier(1L);

        assertThat(supplier.isActive()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 거래처 비활성화 시 SUPPLIER_NOT_FOUND 예외가 발생한다")
    void deactivateSupplier_notFound_throwsException() {
        given(supplierRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.deactivateSupplier(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SUPPLIER_NOT_FOUND));
    }

    // activateSupplier

    @Test
    @DisplayName("activateSupplier() 호출 시 isActive가 true가 된다")
    void activateSupplier_setsIsActiveTrue() {
        Supplier supplier = Supplier.create("(주)테스트", SupplierType.PURCHASE, null, null, null, null);
        supplier.deactivate();
        given(supplierRepository.findById(1L)).willReturn(Optional.of(supplier));

        supplierService.activateSupplier(1L);

        assertThat(supplier.isActive()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 거래처 활성화 시 SUPPLIER_NOT_FOUND 예외가 발생한다")
    void activateSupplier_notFound_throwsException() {
        given(supplierRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.activateSupplier(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SUPPLIER_NOT_FOUND));
    }

    // 헬퍼 메서드

    private SupplierCreateRequest createRequest(String name, SupplierType supplierType) {
        SupplierCreateRequest request = new SupplierCreateRequest();
        request.setName(name);
        request.setSupplierType(supplierType);
        return request;
    }

    private SupplierUpdateRequest updateRequest(String name, SupplierType supplierType,
                                                String contactName, String contactPhone) {
        SupplierUpdateRequest request = new SupplierUpdateRequest();
        request.setName(name);
        request.setSupplierType(supplierType);
        request.setContactName(contactName);
        request.setContactPhone(contactPhone);
        return request;
    }
}
