package com.stockandorder.domain.stock.repository;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.domain.stock.dto.StockListResponse;
import com.stockandorder.domain.stock.dto.StockSearchCondition;
import com.stockandorder.domain.stock.entity.Stock;
import com.stockandorder.domain.stock.enums.StockStatus;
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

import java.math.BigDecimal;
import java.util.List;
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

    @Test
    @DisplayName("search: status=SHORTAGE 필터는 0 < 재고 < 안전재고 인 상품만 반환한다")
    void search_filtersByShortage() {
        Category category = Category.create("필터테스트", null);
        em.persist(category);
        persistProductWithStock("S-OUT", "품절상품", category, 10, 0);   // 품절
        persistProductWithStock("S-SHORT", "미달상품", category, 10, 5);  // 미달
        persistProductWithStock("S-OK", "정상상품", category, 10, 20);    // 정상
        em.flush();
        em.clear();

        StockSearchCondition condition = new StockSearchCondition();
        condition.setStatus(StockStatus.SHORTAGE);

        Page<StockListResponse> result = stockRepository.search(condition, PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(StockListResponse::getProductCode)
                .containsExactly("S-SHORT");
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(StockStatus.SHORTAGE);
    }

    @Test
    @DisplayName("search: status=OUT_OF_STOCK 필터는 재고 0 인 상품만 반환한다")
    void search_filtersByOutOfStock() {
        Category category = Category.create("필터테스트2", null);
        em.persist(category);
        persistProductWithStock("O-SHORT", "미달상품", category, 10, 5);
        persistProductWithStock("O-OUT", "품절상품", category, 10, 0);
        em.flush();
        em.clear();

        StockSearchCondition condition = new StockSearchCondition();
        condition.setStatus(StockStatus.OUT_OF_STOCK);

        Page<StockListResponse> result = stockRepository.search(condition, PageRequest.of(0, 10));

        // setUp의 밀가루(재고 0)도 품절이므로 함께 잡힌다 → 코드로 검증
        assertThat(result.getContent())
                .extracting(StockListResponse::getProductCode)
                .contains("O-OUT")
                .doesNotContain("O-SHORT");
    }

    @Test
    @DisplayName("search: status 필터는 WHERE에서 적용돼 페이징이 정확하다(애플리케이션 필터링 금지 검증)")
    void search_filterWithPaging_keepsTotalAccurate() {
        Category category = Category.create("페이징테스트", null);
        em.persist(category);
        // 미달 3건 + 정상 1건. 미달만 필터 + 페이지 크기 2 → total은 3, 한 페이지는 2여야 한다.
        persistProductWithStock("P-1", "미달1", category, 10, 1);
        persistProductWithStock("P-2", "미달2", category, 10, 2);
        persistProductWithStock("P-3", "미달3", category, 10, 3);
        persistProductWithStock("P-OK", "정상1", category, 10, 50);
        em.flush();
        em.clear();

        StockSearchCondition condition = new StockSearchCondition();
        condition.setCategoryId(category.getCategoryId());
        condition.setStatus(StockStatus.SHORTAGE);

        Page<StockListResponse> result = stockRepository.search(condition, PageRequest.of(0, 2));

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("search: sort=QUANTITY_ASC 는 재고량 오름차순으로 정렬한다")
    void search_sortByQuantityAsc() {
        Category category = Category.create("정렬테스트", null);
        em.persist(category);
        persistProductWithStock("Q-A", "상품A", category, 0, 30);
        persistProductWithStock("Q-B", "상품B", category, 0, 5);
        persistProductWithStock("Q-C", "상품C", category, 0, 15);
        em.flush();
        em.clear();

        StockSearchCondition condition = new StockSearchCondition();
        condition.setCategoryId(category.getCategoryId());
        condition.setSort("QUANTITY_ASC");

        Page<StockListResponse> result = stockRepository.search(condition, PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(StockListResponse::getQuantity)
                .containsExactly(5, 15, 30);
    }

    @Test
    @DisplayName("search: keyword 는 상품명·상품코드를 부분 일치로 검색한다")
    void search_byKeyword() {
        Category category = Category.create("키워드테스트", null);
        em.persist(category);
        persistProductWithStock("KW-001", "특수볼트", category, 0, 10);
        persistProductWithStock("OTHER-1", "너트", category, 0, 10);
        em.flush();
        em.clear();

        StockSearchCondition condition = new StockSearchCondition();
        condition.setKeyword("볼트");

        Page<StockListResponse> result = stockRepository.search(condition, PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(StockListResponse::getProductName)
                .containsExactly("특수볼트");
    }

    private void persistProductWithStock(String code, String name, Category category,
                                         int safetyStock, int quantity) {
        Product p = Product.create(code, name, category, "EA",
                BigDecimal.valueOf(1000), safetyStock, null);
        em.persist(p);
        Stock stock = Stock.create(p);
        if (quantity > 0) {
            stock.increase(quantity);
        }
        em.persist(stock);
    }
}
