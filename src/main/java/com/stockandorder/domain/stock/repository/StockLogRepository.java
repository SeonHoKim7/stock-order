package com.stockandorder.domain.stock.repository;

import com.stockandorder.domain.stock.entity.StockLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockLogRepository extends JpaRepository<StockLog, Long> {

    // (product_id, created_at) 복합 인덱스를 활용한 상품별 최신순 이력 조회
    @Query("SELECT sl FROM StockLog sl WHERE sl.product.productId = :productId ORDER BY sl.createdAt DESC")
    Page<StockLog> findByProductId(@Param("productId") Long productId, Pageable pageable);
}
