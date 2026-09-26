package com.stockandorder.domain.product.repository;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.global.config.JpaConfig;
import com.stockandorder.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * search()의 includeInactive 동작 검증.
 * JPQL의 boolean 파라미터 비교는 컴파일 단계에서 걸러지지 않아 실제로 실행해봐야 한다.
 */
@DataJpaTest
@Import({QuerydslConfig.class, JpaConfig.class})
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager em;

    private final Pageable pageable = PageRequest.of(0, 10);
    private Long categoryId;

    @BeforeEach
    void setUp() {
        Category category = Category.create("전자기기", null);
        em.persist(category);
        categoryId = category.getCategoryId();

        Product active1 = Product.create("ELEC-001", "노트북 14인치", category, "EA",
                BigDecimal.valueOf(950000), 5, null);
        Product active2 = Product.create("ELEC-002", "무선 마우스", category, "EA",
                BigDecimal.valueOf(12000), 20, null);
        Product inactive = Product.create("ELEC-003", "단종 키보드", category, "EA",
                BigDecimal.valueOf(68000), 10, null);
        inactive.deactivate();

        em.persist(active1);
        em.persist(active2);
        em.persist(inactive);
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("기본값은 활성 상품만 반환한다 — 발주·출고 폼의 선택지가 이 쿼리를 쓰기 때문이다")
    void search_excludesInactiveByDefault() {
        Page<Product> result = productRepository.search(null, null, false, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allSatisfy(p -> assertThat(p.isActive()).isTrue());
    }

    @Test
    @DisplayName("includeInactive면 비활성 상품까지 반환한다 — 재활성화하려면 목록에서 찾을 수 있어야 한다")
    void search_includesInactiveWhenRequested() {
        Page<Product> result = productRepository.search(null, null, true, pageable);

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent())
                .extracting(Product::getName)
                .contains("단종 키보드");
    }

    @Test
    @DisplayName("includeInactive는 키워드·카테고리 필터와 함께 동작한다")
    void search_combinesWithOtherFilters() {
        assertThat(productRepository.search("키보드", null, false, pageable).getTotalElements()).isZero();
        assertThat(productRepository.search("키보드", null, true, pageable).getTotalElements()).isEqualTo(1);
        assertThat(productRepository.search(null, categoryId, true, pageable).getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("키워드는 상품명과 상품코드 모두에 적용된다")
    void search_keywordMatchesNameOrCode() {
        assertThat(productRepository.search("노트북", null, false, pageable).getTotalElements()).isEqualTo(1);
        assertThat(productRepository.search("ELEC-002", null, false, pageable).getTotalElements()).isEqualTo(1);
    }
}
