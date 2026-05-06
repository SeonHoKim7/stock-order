package com.stockandorder.domain.stock.repository;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.domain.stock.entity.StockLog;
import com.stockandorder.domain.stock.enums.StockChangeType;
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

@DataJpaTest
@Import({QuerydslConfig.class, JpaConfig.class})
class StockLogRepositoryTest {

    @Autowired
    private StockLogRepository stockLogRepository;

    @Autowired
    private EntityManager em;

    private Product productA;
    private Product productB;

    @BeforeEach
    void setUp() {
        Category category = Category.create("식자재", null);
        em.persist(category);

        productA = Product.create("PRD-A", "밀가루", category, "KG",
                BigDecimal.valueOf(10000), 10, null);
        productB = Product.create("PRD-B", "설탕", category, "KG",
                BigDecimal.valueOf(5000), 5, null);
        em.persist(productA);
        em.persist(productB);

        // productA: INBOUND 10 → OUTBOUND -3 → ADJUST +2 (이력 3건)
        em.persist(StockLog.of(productA, StockChangeType.INBOUND, 10, 0, 10, 1L, null));
        em.persist(StockLog.of(productA, StockChangeType.OUTBOUND, -3, 10, 7, 2L, null));
        em.persist(StockLog.of(productA, StockChangeType.ADJUST, 2, 7, 9, null, "실사 가산"));

        // productB: INBOUND 1건
        em.persist(StockLog.of(productB, StockChangeType.INBOUND, 5, 0, 5, 3L, null));

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("상품 ID로 이력 조회 시 해당 상품의 이력만 반환된다")
    void findByProductId_returnsOnlyMatchingProductLogs() {
        Page<StockLog> result = stockLogRepository.findByProductId(
                productA.getProductId(), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent())
                .allSatisfy(log -> assertThat(log.getProduct().getProductId())
                        .isEqualTo(productA.getProductId()));
    }

    @Test
    @DisplayName("상품 ID로 이력 조회 시 createdAt DESC로 정렬된다")
    void findByProductId_returnsSortedByCreatedAtDesc() {
        Page<StockLog> result = stockLogRepository.findByProductId(
                productA.getProductId(), PageRequest.of(0, 10));

        // setUp 순서대로 INBOUND → OUTBOUND → ADJUST 이므로 최신순은 ADJUST가 먼저
        assertThat(result.getContent().get(0).getChangeType()).isEqualTo(StockChangeType.ADJUST);
        assertThat(result.getContent().get(2).getChangeType()).isEqualTo(StockChangeType.INBOUND);
    }

    @Test
    @DisplayName("페이징 적용 시 size만큼 제한하고 totalElements는 전체 건수를 반환한다")
    void findByProductId_paging() {
        Pageable pageable = PageRequest.of(0, 2);

        Page<StockLog> result = stockLogRepository.findByProductId(
                productA.getProductId(), pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("이력이 없는 상품 ID 조회 시 빈 페이지를 반환한다")
    void findByProductId_noLogs_returnsEmptyPage() {
        Page<StockLog> result = stockLogRepository.findByProductId(
                999L, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
