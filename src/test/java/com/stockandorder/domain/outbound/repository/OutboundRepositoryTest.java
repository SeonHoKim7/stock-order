package com.stockandorder.domain.outbound.repository;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import com.stockandorder.domain.outbound.dto.OutboundListResponse;
import com.stockandorder.domain.outbound.dto.OutboundSearchCondition;
import com.stockandorder.domain.outbound.entity.Outbound;
import com.stockandorder.domain.outbound.entity.OutboundItem;
import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.domain.supplier.entity.Supplier;
import com.stockandorder.domain.supplier.enums.SupplierType;
import com.stockandorder.global.config.JpaConfig;
import com.stockandorder.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QuerydslConfig.class, JpaConfig.class})
class OutboundRepositoryTest {

    @Autowired
    private OutboundRepository outboundRepository;

    @Autowired
    private EntityManager em;

    private Supplier supplierA;
    private Supplier supplierB;
    private Long outbound1Id;

    private static final LocalDate DATE_10 = LocalDate.of(2026, 6, 10);
    private static final LocalDate DATE_11 = LocalDate.of(2026, 6, 11);
    private static final LocalDate DATE_12 = LocalDate.of(2026, 6, 12);

    @BeforeEach
    void setUp() {
        Category category = Category.create("식자재", null);
        em.persist(category);

        Product product = Product.create("PRD-001", "밀가루", category, "KG",
                BigDecimal.valueOf(10000), BigDecimal.valueOf(13000), 10, null);
        em.persist(product);

        supplierA = Supplier.create("판매처A", SupplierType.SALES, "담당자A", null, null, null);
        supplierB = Supplier.create("판매처B", SupplierType.BOTH, "담당자B", null, null, null);
        em.persist(supplierA);
        em.persist(supplierB);

        Member processor = Member.create("manager1", "password", "매니저1", "mgr@test.com", Role.MANAGER);
        em.persist(processor);

        // 출고 3건: supplierA 2건(06-10, 06-12), supplierB 1건(06-11)
        Outbound outbound1 = Outbound.create("OUT-20260610-001", supplierA, processor, DATE_10, "출고1",
                List.of(OutboundItem.create(product, 5, BigDecimal.valueOf(13000))));
        Outbound outbound2 = Outbound.create("OUT-20260612-001", supplierA, processor, DATE_12, "출고2",
                List.of(OutboundItem.create(product, 3, BigDecimal.valueOf(13000))));
        Outbound outbound3 = Outbound.create("OUT-20260611-001", supplierB, processor, DATE_11, "출고3",
                List.of(OutboundItem.create(product, 2, BigDecimal.valueOf(13000))));
        em.persist(outbound1);
        em.persist(outbound2);
        em.persist(outbound3);
        outbound1Id = outbound1.getOutboundId();

        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("조건 없이 조회하면 전체를 outboundDate DESC로 반환한다")
        void search_noCondition_returnsAllByDateDesc() {
            Page<OutboundListResponse> result =
                    outboundRepository.search(new OutboundSearchCondition(), PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getContent()).extracting(OutboundListResponse::getOutboundDate)
                    .containsExactly(DATE_12, DATE_11, DATE_10);
        }

        @Test
        @DisplayName("거래처 필터 적용 시 해당 판매처의 출고만 반환한다")
        void search_supplierFilter_returnsMatching() {
            OutboundSearchCondition condition = new OutboundSearchCondition();
            condition.setSupplierId(supplierB.getSupplierId());

            Page<OutboundListResponse> result =
                    outboundRepository.search(condition, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getSupplierName()).isEqualTo("판매처B");
        }

        @Test
        @DisplayName("기간 필터 적용 시 outboundDate 범위 내 출고만 반환한다")
        void search_dateRangeFilter_returnsWithinRange() {
            OutboundSearchCondition condition = new OutboundSearchCondition();
            condition.setStartDate(DATE_11);
            condition.setEndDate(DATE_12);

            Page<OutboundListResponse> result =
                    outboundRepository.search(condition, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).extracting(OutboundListResponse::getOutboundDate)
                    .containsExactly(DATE_12, DATE_11);
        }

        @Test
        @DisplayName("페이징 적용 시 size만큼 제한하고 totalElements는 전체 건수를 반환한다")
        void search_paging_limitsSize() {
            Page<OutboundListResponse> result =
                    outboundRepository.search(new OutboundSearchCondition(), PageRequest.of(0, 2));

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("DTO에 출고번호/거래처명/처리자명/매출총액이 올바르게 매핑된다")
        void search_dtoFieldMapping() {
            OutboundSearchCondition condition = new OutboundSearchCondition();
            condition.setSupplierId(supplierB.getSupplierId());

            OutboundListResponse dto =
                    outboundRepository.search(condition, PageRequest.of(0, 10)).getContent().get(0);

            assertThat(dto.getOutboundNumber()).isEqualTo("OUT-20260611-001");
            assertThat(dto.getProcessorName()).isEqualTo("매니저1");
            assertThat(dto.getOutboundDate()).isEqualTo(DATE_11);
            // 13,000 * 2 = 26,000
            assertThat(dto.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(26000));
        }
    }

    @Nested
    @DisplayName("findDetailById")
    class FindDetailById {

        @Test
        @DisplayName("출고/거래처/처리자/항목/상품을 한 번에 로딩한다")
        void findDetailById_loadsFullGraph() {
            Optional<Outbound> result = outboundRepository.findDetailById(outbound1Id);

            assertThat(result).isPresent();
            Outbound outbound = result.get();
            assertThat(outbound.getOutboundNumber()).isEqualTo("OUT-20260610-001");
            assertThat(outbound.getSupplier().getName()).isEqualTo("판매처A");
            assertThat(outbound.getProcessor().getName()).isEqualTo("매니저1");
            assertThat(outbound.getItems()).hasSize(1);
            assertThat(outbound.getItems().get(0).getProduct().getName()).isEqualTo("밀가루");
            assertThat(outbound.getItems().get(0).getQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("존재하지 않는 출고 ID는 빈 Optional을 반환한다")
        void findDetailById_nonExisting_returnsEmpty() {
            assertThat(outboundRepository.findDetailById(999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findMaxOutboundNumberByPrefix")
    class FindMaxOutboundNumberByPrefix {

        @Test
        @DisplayName("해당 prefix의 최대 출고번호를 반환한다")
        void findMax_returnsMaxNumber() {
            Optional<String> result = outboundRepository.findMaxOutboundNumberByPrefix("OUT-20260610-");

            assertThat(result).contains("OUT-20260610-001");
        }

        @Test
        @DisplayName("해당 prefix의 출고번호가 없으면 빈 Optional을 반환한다")
        void findMax_noMatch_returnsEmpty() {
            Optional<String> result = outboundRepository.findMaxOutboundNumberByPrefix("OUT-20991231-");

            assertThat(result).isEmpty();
        }
    }
}
