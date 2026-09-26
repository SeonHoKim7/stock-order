package com.stockandorder.domain.supplier.repository;

import com.stockandorder.domain.supplier.entity.Supplier;
import com.stockandorder.domain.supplier.enums.SupplierType;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * search()의 includeInactive 동작 검증. 상품 쪽과 같은 이유로 실제 실행이 필요하다.
 */
@DataJpaTest
@Import({QuerydslConfig.class, JpaConfig.class})
class SupplierRepositoryTest {

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private EntityManager em;

    private final Pageable pageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        Supplier active1 = Supplier.create("대한전자유통", SupplierType.PURCHASE,
                "김대한", "02-1234-5678", null, null);
        Supplier active2 = Supplier.create("푸른마트", SupplierType.SALES,
                "최푸른", "051-333-9900", null, null);
        Supplier inactive = Supplier.create("폐업거래처", SupplierType.PURCHASE,
                "박폐업", "031-000-0000", null, null);
        inactive.deactivate();

        em.persist(active1);
        em.persist(active2);
        em.persist(inactive);
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("기본값은 활성 거래처만 반환한다")
    void search_excludesInactiveByDefault() {
        Page<Supplier> result = supplierRepository.search(null, null, false, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allSatisfy(s -> assertThat(s.isActive()).isTrue());
    }

    @Test
    @DisplayName("includeInactive면 비활성 거래처까지 반환한다")
    void search_includesInactiveWhenRequested() {
        Page<Supplier> result = supplierRepository.search(null, null, true, pageable);

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent())
                .extracting(Supplier::getName)
                .contains("폐업거래처");
    }

    @Test
    @DisplayName("includeInactive는 유형 필터와 함께 동작한다")
    void search_combinesWithTypeFilter() {
        assertThat(supplierRepository.search(null, SupplierType.PURCHASE, false, pageable)
                .getTotalElements()).isEqualTo(1);
        assertThat(supplierRepository.search(null, SupplierType.PURCHASE, true, pageable)
                .getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("findByIsActiveTrue는 등록 폼용이므로 비활성 거래처를 절대 포함하지 않는다")
    void findByIsActiveTrue_neverIncludesInactive() {
        assertThat(supplierRepository.findByIsActiveTrue())
                .hasSize(2)
                .allSatisfy(s -> assertThat(s.isActive()).isTrue());
    }
}
