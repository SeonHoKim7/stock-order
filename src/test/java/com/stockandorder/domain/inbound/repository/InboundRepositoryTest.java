package com.stockandorder.domain.inbound.repository;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.inbound.dto.InboundListResponse;
import com.stockandorder.domain.inbound.dto.InboundSearchCondition;
import com.stockandorder.domain.inbound.entity.Inbound;
import com.stockandorder.domain.inbound.entity.InboundItem;
import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import com.stockandorder.domain.order.entity.PurchaseOrder;
import com.stockandorder.domain.order.entity.PurchaseOrderItem;
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
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QuerydslConfig.class, JpaConfig.class})
class InboundRepositoryTest {

    @Autowired
    private InboundRepository inboundRepository;

    @Autowired
    private EntityManager em;

    private Supplier supplierA;
    private Supplier supplierB;
    private Long inbound1Id;

    private static final LocalDate DATE_10 = LocalDate.of(2026, 6, 10);
    private static final LocalDate DATE_11 = LocalDate.of(2026, 6, 11);
    private static final LocalDate DATE_12 = LocalDate.of(2026, 6, 12);

    @BeforeEach
    void setUp() {
        Category category = Category.create("식자재", null);
        em.persist(category);

        Product product = Product.create("PRD-001", "밀가루", category, "KG",
                BigDecimal.valueOf(10000), 10, null);
        em.persist(product);

        supplierA = Supplier.create("공급처A", SupplierType.PURCHASE, "담당자A", null, null, null);
        supplierB = Supplier.create("공급처B", SupplierType.BOTH, "담당자B", null, null, null);
        em.persist(supplierA);
        em.persist(supplierB);

        Member requester = Member.create("staff1", "password", "직원1", "staff1@test.com", Role.STAFF);
        Member processor = Member.create("manager1", "password", "매니저1", "mgr@test.com", Role.MANAGER);
        em.persist(requester);
        em.persist(processor);

        // 발주 2건(supplierA, supplierB). 입고 처리를 위해 승인 상태로 둔다.
        PurchaseOrderItem orderItemA = PurchaseOrderItem.create(product, 100, BigDecimal.valueOf(10000));
        PurchaseOrder orderA = PurchaseOrder.create("PO-20260610-001", supplierA, requester, null);
        orderA.addItem(orderItemA);
        orderA.approve(processor);
        em.persist(orderA);

        PurchaseOrderItem orderItemB = PurchaseOrderItem.create(product, 50, BigDecimal.valueOf(10000));
        PurchaseOrder orderB = PurchaseOrder.create("PO-20260610-002", supplierB, requester, null);
        orderB.addItem(orderItemB);
        orderB.approve(processor);
        em.persist(orderB);

        // 입고 3건: supplierA 2건(06-10, 06-12), supplierB 1건(06-11)
        Inbound inbound1 = Inbound.create("IN-20260610-001", orderA, processor, DATE_10, "입고1",
                List.of(InboundItem.create(orderItemA, 10)));
        Inbound inbound2 = Inbound.create("IN-20260612-001", orderA, processor, DATE_12, "입고2",
                List.of(InboundItem.create(orderItemA, 20)));
        Inbound inbound3 = Inbound.create("IN-20260611-001", orderB, processor, DATE_11, "입고3",
                List.of(InboundItem.create(orderItemB, 5)));
        em.persist(inbound1);
        em.persist(inbound2);
        em.persist(inbound3);
        inbound1Id = inbound1.getInboundId();

        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("조건 없이 조회하면 전체를 inboundDate DESC로 반환한다")
        void search_noCondition_returnsAllByDateDesc() {
            Page<InboundListResponse> result =
                    inboundRepository.search(new InboundSearchCondition(), PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(3);
            // inboundDate DESC: 06-12 → 06-11 → 06-10
            assertThat(result.getContent()).extracting(InboundListResponse::getInboundDate)
                    .containsExactly(DATE_12, DATE_11, DATE_10);
        }

        @Test
        @DisplayName("거래처 필터 적용 시 해당 거래처(발주 경유)의 입고만 반환한다")
        void search_supplierFilter_returnsMatching() {
            InboundSearchCondition condition = new InboundSearchCondition();
            condition.setSupplierId(supplierB.getSupplierId());

            Page<InboundListResponse> result =
                    inboundRepository.search(condition, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getSupplierName()).isEqualTo("공급처B");
        }

        @Test
        @DisplayName("기간 필터 적용 시 inboundDate 범위 내 입고만 반환한다")
        void search_dateRangeFilter_returnsWithinRange() {
            InboundSearchCondition condition = new InboundSearchCondition();
            condition.setStartDate(DATE_11);
            condition.setEndDate(DATE_12);

            Page<InboundListResponse> result =
                    inboundRepository.search(condition, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).extracting(InboundListResponse::getInboundDate)
                    .containsExactly(DATE_12, DATE_11);
        }

        @Test
        @DisplayName("페이징 적용 시 size만큼 제한하고 totalElements는 전체 건수를 반환한다")
        void search_paging_limitsSize() {
            Page<InboundListResponse> result =
                    inboundRepository.search(new InboundSearchCondition(), PageRequest.of(0, 2));

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("DTO에 입고번호/발주번호/거래처명/처리자명이 올바르게 매핑된다")
        void search_dtoFieldMapping() {
            InboundSearchCondition condition = new InboundSearchCondition();
            condition.setSupplierId(supplierB.getSupplierId());

            InboundListResponse dto =
                    inboundRepository.search(condition, PageRequest.of(0, 10)).getContent().get(0);

            assertThat(dto.getInboundNumber()).isEqualTo("IN-20260611-001");
            assertThat(dto.getOrderNumber()).isEqualTo("PO-20260610-002");
            assertThat(dto.getProcessorName()).isEqualTo("매니저1");
            assertThat(dto.getInboundDate()).isEqualTo(DATE_11);
        }
    }

    @Nested
    @DisplayName("findDetailById")
    class FindDetailById {

        @Test
        @DisplayName("입고/발주/거래처/처리자/항목/발주항목/상품을 한 번에 로딩한다")
        void findDetailById_loadsFullGraph() {
            Optional<Inbound> result = inboundRepository.findDetailById(inbound1Id);

            assertThat(result).isPresent();
            Inbound inbound = result.get();
            assertThat(inbound.getInboundNumber()).isEqualTo("IN-20260610-001");
            assertThat(inbound.getPurchaseOrder().getOrderNumber()).isEqualTo("PO-20260610-001");
            assertThat(inbound.getPurchaseOrder().getSupplier().getName()).isEqualTo("공급처A");
            assertThat(inbound.getProcessor().getName()).isEqualTo("매니저1");
            assertThat(inbound.getItems()).hasSize(1);
            assertThat(inbound.getItems().get(0).getOrderItem().getProduct().getName()).isEqualTo("밀가루");
            assertThat(inbound.getItems().get(0).getQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("존재하지 않는 입고 ID는 빈 Optional을 반환한다")
        void findDetailById_nonExisting_returnsEmpty() {
            assertThat(inboundRepository.findDetailById(999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findMaxInboundNumberByPrefix")
    class FindMaxInboundNumberByPrefix {

        @Test
        @DisplayName("해당 prefix의 최대 입고번호를 반환한다")
        void findMax_returnsMaxNumber() {
            Optional<String> result = inboundRepository.findMaxInboundNumberByPrefix("IN-20260610-");

            assertThat(result).contains("IN-20260610-001");
        }

        @Test
        @DisplayName("해당 prefix의 입고번호가 없으면 빈 Optional을 반환한다")
        void findMax_noMatch_returnsEmpty() {
            Optional<String> result = inboundRepository.findMaxInboundNumberByPrefix("IN-20991231-");

            // MAX 집계 쿼리는 행이 없으면 NULL → Optional.empty
            assertThat(result).isEmpty();
        }
    }
}
