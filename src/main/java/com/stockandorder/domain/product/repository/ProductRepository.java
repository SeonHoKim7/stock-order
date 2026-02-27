package com.stockandorder.domain.product.repository;

import com.stockandorder.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByProductCode(String productCode);

    boolean existsByProductCodeAndProductIdNot(String productCode, Long productId);

    // CategoryService.deleteCategory()에서 삭제 가능 여부 확인 시 사용
    boolean existsByCategoryCategoryId(Long categoryId);

    // 키워드(상품명/코드), 카테고리 필터 + 활성 상품만 조회
    // keyword가 null이면 전체 조회 (JPQL에서 :keyword IS NULL 조건 처리)
    @Query("SELECT p FROM Product p LEFT JOIN p.category c " +
           "WHERE (:keyword IS NULL OR p.name LIKE %:keyword% OR p.productCode LIKE %:keyword%) " +
           "AND (:categoryId IS NULL OR c.categoryId = :categoryId) " +
           "AND p.isActive = true")
    Page<Product> search(@Param("keyword") String keyword,
                         @Param("categoryId") Long categoryId,
                         Pageable pageable);
}
