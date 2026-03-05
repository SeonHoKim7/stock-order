package com.stockandorder.domain.order.repository;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import com.stockandorder.domain.order.dto.PurchaseOrderListResponse;
import com.stockandorder.domain.order.dto.PurchaseOrderSearchCondition;
import com.stockandorder.domain.order.entity.PurchaseOrder;
import com.stockandorder.domain.order.entity.PurchaseOrderItem;
import com.stockandorder.domain.order.enums.OrderStatus;
import com.stockandorder.domain.product.entity.Product;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QuerydslConfig.class, JpaConfig.class})
class PurchaseOrderRepositoryImplTest {

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private EntityManager em;

    private Supplier supplierA;
    private Supplier supplierB;
    private Member requester;

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

        requester = Member.create("staff1", "password", "직원1", "staff1@test.com", Role.STAFF);
        em.persist(requester);

        // 발주 3건: supplierA 2건(PENDING, APPROVED), supplierB 1건(PENDING)
        PurchaseOrder order1 = PurchaseOrder.create("PO-20260301-001", supplierA, requester, "발주1");
        order1.addItem(PurchaseOrderItem.create(product, 10, BigDecimal.valueOf(10000)));
        em.persist(order1);

        PurchaseOrder order2 = PurchaseOrder.create("PO-20260302-001", supplierA, requester, "발주2");
        order2.addItem(PurchaseOrderItem.create(product, 5, BigDecimal.valueOf(10000)));
        em.persist(order2);

        Member approver = Member.create("manager1", "password", "매니저1", "mgr@test.com", Role.MANAGER);
        em.persist(approver);
        order2.approve(approver);

        PurchaseOrder order3 = PurchaseOrder.create("PO-20260303-001", supplierB, requester, "발주3");
        order3.addItem(PurchaseOrderItem.create(product, 3, BigDecimal.valueOf(10000)));
        em.persist(order3);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("조건 없이 전체 조회 시 모든 발주를 orderedAt DESC로 반환한다")
    void search_noCondition_returnsAllOrderedByDateDesc() {
        PurchaseOrderSearchCondition condition = new PurchaseOrderSearchCondition();
        Pageable pageable = PageRequest.of(0, 10);

        Page<PurchaseOrderListResponse> result = purchaseOrderRepository.search(condition, pageable);

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(3);
        // orderedAt DESC이므로 가장 최근 발주가 먼저
        assertThat(result.getContent().get(0).getOrderNumber()).isEqualTo("PO-20260303-001");
    }

    @Test
    @DisplayName("상태 필터 적용 시 해당 상태만 반환한다")
    void search_statusFilter_returnsMatchingStatus() {
        PurchaseOrderSearchCondition condition = new PurchaseOrderSearchCondition();
        condition.setStatus(OrderStatus.APPROVED);
        Pageable pageable = PageRequest.of(0, 10);

        Page<PurchaseOrderListResponse> result = purchaseOrderRepository.search(condition, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("기간 필터 적용 시 범위 내 발주만 반환한다")
    void search_dateFilter_returnsWithinRange() {
        // orderedAt은 PurchaseOrder.create() 시점의 LocalDateTime.now()이므로 오늘 날짜 기준
        LocalDate today = LocalDate.now();
        PurchaseOrderSearchCondition condition = new PurchaseOrderSearchCondition();
        condition.setStartDate(today);
        condition.setEndDate(today);
        Pageable pageable = PageRequest.of(0, 10);

        Page<PurchaseOrderListResponse> result = purchaseOrderRepository.search(condition, pageable);

        // 3건 모두 오늘 생성되었으므로 3건 반환
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("미래 날짜로 기간 필터 적용 시 결과가 없다")
    void search_futureDateFilter_returnsEmpty() {
        PurchaseOrderSearchCondition condition = new PurchaseOrderSearchCondition();
        condition.setStartDate(LocalDate.now().plusDays(1));
        Pageable pageable = PageRequest.of(0, 10);

        Page<PurchaseOrderListResponse> result = purchaseOrderRepository.search(condition, pageable);

        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("거래처 필터 적용 시 해당 거래처의 발주만 반환한다")
    void search_supplierFilter_returnsMatchingSupplier() {
        PurchaseOrderSearchCondition condition = new PurchaseOrderSearchCondition();
        condition.setSupplierId(supplierB.getSupplierId());
        Pageable pageable = PageRequest.of(0, 10);

        Page<PurchaseOrderListResponse> result = purchaseOrderRepository.search(condition, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getSupplierName()).isEqualTo("공급처B");
    }

    @Test
    @DisplayName("복합 조건(상태 + 거래처) 적용 시 AND 조건으로 필터링한다")
    void search_multipleConditions_appliesAndLogic() {
        PurchaseOrderSearchCondition condition = new PurchaseOrderSearchCondition();
        condition.setStatus(OrderStatus.PENDING);
        condition.setSupplierId(supplierA.getSupplierId());
        Pageable pageable = PageRequest.of(0, 10);

        Page<PurchaseOrderListResponse> result = purchaseOrderRepository.search(condition, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getOrderNumber()).isEqualTo("PO-20260301-001");
    }

    @Test
    @DisplayName("페이징 적용 시 size만큼 제한하고 totalElements는 전체 건수를 반환한다")
    void search_paging_limitsSizeAndReturnsTotalElements() {
        PurchaseOrderSearchCondition condition = new PurchaseOrderSearchCondition();
        Pageable pageable = PageRequest.of(0, 2);

        Page<PurchaseOrderListResponse> result = purchaseOrderRepository.search(condition, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("조건에 맞는 결과가 없으면 빈 페이지를 반환한다")
    void search_noMatch_returnsEmptyPage() {
        PurchaseOrderSearchCondition condition = new PurchaseOrderSearchCondition();
        condition.setStatus(OrderStatus.CANCELLED);
        Pageable pageable = PageRequest.of(0, 10);

        Page<PurchaseOrderListResponse> result = purchaseOrderRepository.search(condition, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("DTO 필드에 supplierName, requesterName이 올바르게 매핑된다")
    void search_dtoFieldMapping_containsJoinedFields() {
        PurchaseOrderSearchCondition condition = new PurchaseOrderSearchCondition();
        condition.setStatus(OrderStatus.APPROVED);
        Pageable pageable = PageRequest.of(0, 10);

        Page<PurchaseOrderListResponse> result = purchaseOrderRepository.search(condition, pageable);

        PurchaseOrderListResponse dto = result.getContent().get(0);
        assertThat(dto.getSupplierName()).isEqualTo("공급처A");
        assertThat(dto.getRequesterName()).isEqualTo("직원1");
        assertThat(dto.getStatus()).isEqualTo("APPROVED");
        assertThat(dto.getStatusLabel()).isEqualTo("승인");
        assertThat(dto.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(dto.getOrderNumber()).isNotNull();
        assertThat(dto.getOrderedAt()).isNotNull();
    }
}
