package com.stockandorder.domain.order.service;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import com.stockandorder.domain.member.repository.MemberRepository;
import com.stockandorder.domain.order.dto.PurchaseOrderCreateRequest;
import com.stockandorder.domain.order.dto.PurchaseOrderListResponse;
import com.stockandorder.domain.order.dto.PurchaseOrderResponse;
import com.stockandorder.domain.order.dto.PurchaseOrderSearchCondition;
import com.stockandorder.domain.order.entity.PurchaseOrder;
import com.stockandorder.domain.order.entity.PurchaseOrderItem;
import com.stockandorder.domain.order.enums.OrderStatus;
import com.stockandorder.domain.order.repository.PurchaseOrderRepository;
import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.domain.product.repository.ProductRepository;
import com.stockandorder.domain.supplier.entity.Supplier;
import com.stockandorder.domain.supplier.enums.SupplierType;
import com.stockandorder.domain.supplier.repository.SupplierRepository;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProductRepository productRepository;

    private Supplier purchaseSupplier;
    private Supplier salesSupplier;
    private Supplier inactiveSupplier;
    private Member requester;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        purchaseSupplier = Supplier.create("공급처A", SupplierType.PURCHASE,
                "담당자", "010-1234-5678", "test@test.com", "서울");
        salesSupplier = Supplier.create("판매처B", SupplierType.SALES,
                "담당자", "010-1234-5678", "test@test.com", "서울");
        inactiveSupplier = Supplier.create("비활성거래처", SupplierType.PURCHASE,
                "담당자", "010-1234-5678", "test@test.com", "서울");
        inactiveSupplier.deactivate();

        requester = Member.create("staff1", "password", "직원1", "staff1@test.com", Role.STAFF);

        Category category = Category.create("식자재", null);
        product1 = Product.create("PRD-001", "밀가루", category, "KG",
                BigDecimal.valueOf(10000), 10, null);
        product2 = Product.create("PRD-002", "설탕", category, "KG",
                BigDecimal.valueOf(5000), 5, null);
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("정상 요청 시 발주가 생성되고 항목의 unitPrice는 Product에서 스냅샷된다")
        void createOrder_validRequest_createsOrderWithSnapshotPrice() {
            given(supplierRepository.findById(1L)).willReturn(Optional.of(purchaseSupplier));
            given(memberRepository.findById(1L)).willReturn(Optional.of(requester));
            given(productRepository.findById(10L)).willReturn(Optional.of(product1));
            given(productRepository.findById(20L)).willReturn(Optional.of(product2));
            given(purchaseOrderRepository.findMaxOrderNumberByPrefix(anyString())).willReturn(Optional.empty());
            given(purchaseOrderRepository.save(any(PurchaseOrder.class))).willAnswer(invocation -> invocation.getArgument(0));

            PurchaseOrderCreateRequest request = createRequest(1L, "테스트 발주",
                    List.of(createItemRequest(10L, 10), createItemRequest(20L, 20)));

            purchaseOrderService.createOrder(request, 1L);

            ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
            then(purchaseOrderRepository).should().save(captor.capture());

            PurchaseOrder saved = captor.getValue();
            assertThat(saved.getOrderNumber()).startsWith("PO-");
            assertThat(saved.getSupplier()).isEqualTo(purchaseSupplier);
            assertThat(saved.getRequester()).isEqualTo(requester);
            assertThat(saved.getNote()).isEqualTo("테스트 발주");
            assertThat(saved.getItems()).hasSize(2);
            // 10 * 10,000 + 20 * 5,000 = 200,000
            assertThat(saved.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(200000));

            // unitPrice가 Product에서 스냅샷되었는지 확인
            assertThat(saved.getItems().get(0).getUnitPrice())
                    .isEqualByComparingTo(product1.getUnitPrice());
            assertThat(saved.getItems().get(1).getUnitPrice())
                    .isEqualByComparingTo(product2.getUnitPrice());
        }

        @Test
        @DisplayName("기존 발주번호가 있을 경우 다음 번호로 채번된다")
        void createOrder_existingOrderNumber_incrementsSequence() {
            given(supplierRepository.findById(1L)).willReturn(Optional.of(purchaseSupplier));
            given(memberRepository.findById(1L)).willReturn(Optional.of(requester));
            given(productRepository.findById(10L)).willReturn(Optional.of(product1));
            given(purchaseOrderRepository.findMaxOrderNumberByPrefix(anyString()))
                    .willReturn(Optional.of("PO-20260305-005"));
            given(purchaseOrderRepository.save(any(PurchaseOrder.class))).willAnswer(invocation -> invocation.getArgument(0));

            PurchaseOrderCreateRequest request = createRequest(1L, null,
                    List.of(createItemRequest(10L, 1)));

            purchaseOrderService.createOrder(request, 1L);

            ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
            then(purchaseOrderRepository).should().save(captor.capture());
            assertThat(captor.getValue().getOrderNumber()).endsWith("-006");
        }
    }

    // Supplier 검증

    @Nested
    @DisplayName("Supplier 검증")
    class SupplierValidation {

        @Test
        @DisplayName("존재하지 않는 거래처로 발주 생성 시 SUPPLIER_NOT_FOUND 예외가 발생한다")
        void createOrder_supplierNotFound_throwsException() {
            given(supplierRepository.findById(999L)).willReturn(Optional.empty());

            PurchaseOrderCreateRequest request = createRequest(999L, null,
                    List.of(createItemRequest(10L, 1)));

            assertThatThrownBy(() -> purchaseOrderService.createOrder(request, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SUPPLIER_NOT_FOUND));
        }

        @Test
        @DisplayName("판매처(SALES)로 발주 생성 시 SUPPLIER_TYPE_INVALID 예외가 발생한다")
        void createOrder_salesSupplier_throwsException() {
            given(supplierRepository.findById(1L)).willReturn(Optional.of(salesSupplier));

            PurchaseOrderCreateRequest request = createRequest(1L, null,
                    List.of(createItemRequest(10L, 1)));

            assertThatThrownBy(() -> purchaseOrderService.createOrder(request, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SUPPLIER_TYPE_INVALID));
        }

        @Test
        @DisplayName("비활성 거래처로 발주 생성 시 SUPPLIER_INACTIVE 예외가 발생한다")
        void createOrder_inactiveSupplier_throwsException() {
            given(supplierRepository.findById(1L)).willReturn(Optional.of(inactiveSupplier));

            PurchaseOrderCreateRequest request = createRequest(1L, null,
                    List.of(createItemRequest(10L, 1)));

            assertThatThrownBy(() -> purchaseOrderService.createOrder(request, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SUPPLIER_INACTIVE));
        }
    }

    // Member / Product 검증

    @Test
    @DisplayName("존재하지 않는 요청자로 발주 생성 시 MEMBER_NOT_FOUND 예외가 발생한다")
    void createOrder_memberNotFound_throwsException() {
        given(supplierRepository.findById(1L)).willReturn(Optional.of(purchaseSupplier));
        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        PurchaseOrderCreateRequest request = createRequest(1L, null,
                List.of(createItemRequest(10L, 1)));

        assertThatThrownBy(() -> purchaseOrderService.createOrder(request, 999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 발주 생성 시 PRODUCT_NOT_FOUND 예외가 발생한다")
    void createOrder_productNotFound_throwsException() {
        given(supplierRepository.findById(1L)).willReturn(Optional.of(purchaseSupplier));
        given(memberRepository.findById(1L)).willReturn(Optional.of(requester));
        given(purchaseOrderRepository.findMaxOrderNumberByPrefix(anyString())).willReturn(Optional.empty());
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        PurchaseOrderCreateRequest request = createRequest(1L, null,
                List.of(createItemRequest(999L, 1)));

        assertThatThrownBy(() -> purchaseOrderService.createOrder(request, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND));
    }

    // searchOrders

    @Nested
    @DisplayName("searchOrders")
    class SearchOrders {

        @Test
        @DisplayName("condition과 pageable을 repository에 위임하고 결과를 그대로 반환한다")
        void searchOrders_delegatesToRepository() {
            PurchaseOrderSearchCondition condition = new PurchaseOrderSearchCondition();
            condition.setStatus(OrderStatus.PENDING);
            Pageable pageable = PageRequest.of(0, 10);

            PurchaseOrderListResponse listResponse = new PurchaseOrderListResponse(
                    1L, "PO-20260306-001", "공급처A", "직원1",
                    OrderStatus.PENDING, BigDecimal.valueOf(100000), java.time.LocalDateTime.now()
            );
            Page<PurchaseOrderListResponse> expectedPage = new PageImpl<>(List.of(listResponse), pageable, 1);
            given(purchaseOrderRepository.search(condition, pageable)).willReturn(expectedPage);

            Page<PurchaseOrderListResponse> result = purchaseOrderService.searchOrders(condition, pageable);

            assertThat(result).isSameAs(expectedPage);
            then(purchaseOrderRepository).should().search(condition, pageable);
        }
    }

    // getOrder

    @Nested
    @DisplayName("getOrder")
    class GetOrder {

        @Test
        @DisplayName("존재하는 발주 조회 시 PurchaseOrderResponse를 반환한다")
        void getOrder_existingOrder_returnsResponse() {
            PurchaseOrder order = PurchaseOrder.create("PO-20260306-001", purchaseSupplier, requester, "테스트");
            PurchaseOrderItem item = PurchaseOrderItem.create(product1, 5, BigDecimal.valueOf(10000));
            order.addItem(item);

            given(purchaseOrderRepository.findById(1L)).willReturn(Optional.of(order));

            PurchaseOrderResponse result = purchaseOrderService.getOrder(1L);

            assertThat(result.getOrderNumber()).isEqualTo("PO-20260306-001");
            assertThat(result.getSupplierName()).isEqualTo("공급처A");
            assertThat(result.getRequesterName()).isEqualTo("직원1");
            assertThat(result.getStatus()).isEqualTo("PENDING");
            assertThat(result.getStatusLabel()).isEqualTo("대기");
            assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
            assertThat(result.getNote()).isEqualTo("테스트");
            assertThat(result.getApproverName()).isNull();
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getProductName()).isEqualTo("밀가루");
            assertThat(result.getItems().get(0).getQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("존재하지 않는 발주 조회 시 ORDER_NOT_FOUND 예외가 발생한다")
        void getOrder_notFound_throwsException() {
            given(purchaseOrderRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> purchaseOrderService.getOrder(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_NOT_FOUND));
        }
    }

    // 헬퍼 메서드

    private PurchaseOrderCreateRequest createRequest(Long supplierId, String note,
                                                     List<PurchaseOrderCreateRequest.ItemRequest> items) {
        PurchaseOrderCreateRequest request = new PurchaseOrderCreateRequest();
        request.setSupplierId(supplierId);
        request.setNote(note);
        request.setItems(items);
        return request;
    }

    private PurchaseOrderCreateRequest.ItemRequest createItemRequest(Long productId, int quantity) {
        PurchaseOrderCreateRequest.ItemRequest item = new PurchaseOrderCreateRequest.ItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }
}
