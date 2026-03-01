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

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public Page<SupplierResponse> searchSuppliers(String keyword, SupplierType supplierType, Pageable pageable) {
        String kw = (keyword != null && keyword.isBlank()) ? null : keyword;
        return supplierRepository.search(kw, supplierType, pageable)
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
