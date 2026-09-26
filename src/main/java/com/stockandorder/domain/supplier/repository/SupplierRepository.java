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

    // 키워드(거래처명/담당자명), 거래처 유형 필터.
    // includeInactive: 기본은 활성 거래처만. 비활성화한 거래처를 되살리려면 목록에서 찾을 수 있어야 하므로
    // 관리 화면에서만 true로 넘긴다(발주·출고 폼의 거래처 선택지는 findByIsActiveTrue를 따로 쓴다).
    @Query("SELECT s FROM Supplier s " +
           "WHERE (:keyword IS NULL OR s.name LIKE %:keyword% OR s.contactName LIKE %:keyword%) " +
           "AND (:supplierType IS NULL OR s.supplierType = :supplierType) " +
           "AND (:includeInactive = true OR s.isActive = true)")
    Page<Supplier> search(@Param("keyword") String keyword,
                          @Param("supplierType") SupplierType supplierType,
                          @Param("includeInactive") boolean includeInactive,
                          Pageable pageable);
}
