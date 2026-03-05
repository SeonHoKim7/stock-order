package com.stockandorder.domain.supplier.repository;

import com.stockandorder.domain.supplier.entity.Supplier;
import com.stockandorder.domain.supplier.enums.SupplierType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByIsActiveTrue();

    // 키워드(거래처명/담당자명), 거래처 유형 필터 + 활성 거래처만 조회
    @Query("SELECT s FROM Supplier s " +
           "WHERE (:keyword IS NULL OR s.name LIKE %:keyword% OR s.contactName LIKE %:keyword%) " +
           "AND (:supplierType IS NULL OR s.supplierType = :supplierType) " +
           "AND s.isActive = true")
    Page<Supplier> search(@Param("keyword") String keyword,
                          @Param("supplierType") SupplierType supplierType,
                          Pageable pageable);
}
