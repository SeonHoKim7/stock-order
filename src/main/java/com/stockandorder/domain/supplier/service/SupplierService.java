package com.stockandorder.domain.supplier.service;

import com.stockandorder.domain.supplier.dto.SupplierCreateRequest;
import com.stockandorder.domain.supplier.dto.SupplierResponse;
import com.stockandorder.domain.supplier.dto.SupplierUpdateRequest;
import com.stockandorder.domain.supplier.entity.Supplier;
import com.stockandorder.domain.supplier.enums.SupplierType;
import com.stockandorder.domain.supplier.repository.SupplierRepository;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public List<SupplierResponse> getActiveSuppliers() {
        return supplierRepository.findByIsActiveTrue().stream()
                .map(SupplierResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<SupplierResponse> searchSuppliers(String keyword, SupplierType supplierType, Pageable pageable) {
        return searchSuppliers(keyword, supplierType, false, pageable);
    }

    /**
     * @param includeInactive true면 비활성 거래처까지 함께 반환한다. 거래처 관리 화면에서
     *                        비활성화한 거래처를 다시 찾아 활성화하려면 목록에 나타나야 하므로 필요하다.
     */
    @Transactional(readOnly = true)
    public Page<SupplierResponse> searchSuppliers(String keyword, SupplierType supplierType,
                                                  boolean includeInactive, Pageable pageable) {
        String kw = (keyword != null && keyword.isBlank()) ? null : keyword;
        return supplierRepository.search(kw, supplierType, includeInactive, pageable)
                .map(SupplierResponse::from);
    }

    @Transactional(readOnly = true)
    public SupplierResponse getSupplier(Long supplierId) {
        return SupplierResponse.from(findById(supplierId));
    }

    public void createSupplier(SupplierCreateRequest request) {
        supplierRepository.save(Supplier.create(
                request.getName(),
                request.getSupplierType(),
                request.getContactName(),
                request.getContactPhone(),
                request.getContactEmail(),
                request.getAddress()
        ));
    }

    public void updateSupplier(Long supplierId, SupplierUpdateRequest request) {
        Supplier supplier = findById(supplierId);
        supplier.update(
                request.getName(),
                request.getSupplierType(),
                request.getContactName(),
                request.getContactPhone(),
                request.getContactEmail(),
                request.getAddress()
        );
    }

    public void deactivateSupplier(Long supplierId) {
        findById(supplierId).deactivate();
    }

    public void activateSupplier(Long supplierId) {
        findById(supplierId).activate();
    }

    private Supplier findById(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUPPLIER_NOT_FOUND));
    }
}
