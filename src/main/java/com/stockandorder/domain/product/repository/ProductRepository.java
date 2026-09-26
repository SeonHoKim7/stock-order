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

    // 키워드(상품명/코드), 카테고리 필터. keyword가 null이면 전체 조회(JPQL에서 :keyword IS NULL 조건 처리)
    //
    // includeInactive: 기본은 활성 상품만 반환한다. 발주·출고 등록 폼의 상품 목록이 이 쿼리를 쓰기 때문에,
    // 기본값이 "활성만"이어야 비활성 상품이 선택지에 노출되지 않는다.
    // 관리 화면에서 비활성 상품을 되살리려면 목록에서 찾을 수 있어야 하므로, 그때만 true로 넘긴다.
    @Query("SELECT p FROM Product p LEFT JOIN p.category c " +
           "WHERE (:keyword IS NULL OR p.name LIKE %:keyword% OR p.productCode LIKE %:keyword%) " +
           "AND (:categoryId IS NULL OR c.categoryId = :categoryId) " +
           "AND (:includeInactive = true OR p.isActive = true)")
    Page<Product> search(@Param("keyword") String keyword,
                         @Param("categoryId") Long categoryId,
                         @Param("includeInactive") boolean includeInactive,
                         Pageable pageable);
}
