package com.stockandorder.domain.stock.repository;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.domain.stock.entity.Stock;
import com.stockandorder.global.config.JpaConfig;
import com.stockandorder.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({QuerydslConfig.class, JpaConfig.class})
class StockRepositoryTest {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private EntityManager em;

    private Product product;

    @BeforeEach
    void setUp() {
        Category category = Category.create("식자재", null);
        em.persist(category);

        product = Product.create("PRD-001", "밀가루", category, "KG",
                BigDecimal.valueOf(10000), 10, null);
        em.persist(product);

        em.persist(Stock.create(product));

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("findByProductId: 상품 ID로 재고를 조회한다")
    void findByProductId_returnsStock() {
        Optional<Stock> result = stockRepository.findByProductId(product.getProductId());

        assertThat(result).isPresent();
        assertThat(result.get().getQuantity()).isZero();
        assertThat(result.get().getProduct().getProductId()).isEqualTo(product.getProductId());
    }

    @Test
    @DisplayName("findByProductId: 존재하지 않는 상품 ID는 빈 Optional 반환")
    void findByProductId_nonExisting_returnsEmpty() {
        Optional<Stock> result = stockRepository.findByProductId(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByProductIdForUpdate: 비관적 락과 함께 재고를 조회한다")
    void findByProductIdForUpdate_returnsStock() {
        Optional<Stock> result = stockRepository.findByProductIdForUpdate(product.getProductId());

        assertThat(result).isPresent();
        assertThat(result.get().getQuantity()).isZero();
    }

    @Test
    @DisplayName("DB CHECK 제약: quantity 음수 INSERT는 실패한다 (엔티티 메서드 우회 시 최후 방어선)")
    void checkConstraint_negativeQuantity_failsInsert() {
        // setUp의 product는 이미 stock과 1:1로 묶여 있으므로 CHECK 검증을 위해 별도 상품 생성
        Category category = Category.create("기타", null);
        em.persist(category);
        Product fresh = Product.create("PRD-CHECK", "테스트상품", category, "EA",
                BigDecimal.valueOf(1000), 0, null);
        em.persist(fresh);
        em.flush();

        assertThatThrownBy(() -> {
            em.createNativeQuery(
                            "INSERT INTO stock (product_id, quantity, created_at, updated_at) " +
                            "VALUES (:pid, :q, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                    .setParameter("pid", fresh.getProductId())
                    .setParameter("q", -1)
                    .executeUpdate();
            em.flush();
        }).isInstanceOf(Exception.class);
    }
}
