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
import com.stockandorder.domain.supplier.entity.Supplier;
import com.stockandorder.domain.supplier.enums.SupplierType;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * 발주 생성의 재시도 정책과, 조회·상태 변경을 검증한다.
 * 발주 생성 1회의 내부 동작(채번, 거래처/상품 검증 등)은 PurchaseOrderProcessorTest에서 다룬다.
 */
@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    @Mock
    private PurchaseOrderProcessor purchaseOrderProcessor;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private MemberRepository memberRepository;

    private Supplier purchaseSupplier;
    private Member requester;
    private Member manager;
    private Product product1;

    @BeforeEach
    void setUp() {
        purchaseSupplier = Supplier.create("공급처A", SupplierType.PURCHASE,
                "담당자", "010-1234-5678", "test@test.com", "서울");

        requester = Member.create("staff1", "password", "직원1", "staff1@test.com", Role.STAFF);
        ReflectionTestUtils.setField(requester, "memberId", 1L);

        manager = Member.create("manager1", "password", "매니저1", "manager1@test.com", Role.MANAGER);
        ReflectionTestUtils.setField(manager, "memberId", 2L);

        Category category = Category.create("식자재", null);
        product1 = Product.create("PRD-001", "밀가루", category, "KG",
                BigDecimal.valueOf(10000), 10, null);
    }

    @Nested
    @DisplayName("createOrder (발주번호 충돌 재시도 정책)")
    class CreateOrder {

        @Test
        @DisplayName("첫 시도에 성공하면 createOnce를 1회만 호출하고 id를 반환한다")
        void createOrder_succeedsFirstTry() {
            PurchaseOrderCreateRequest request = createRequest();
            given(purchaseOrderProcessor.createOnce(any(), any())).willReturn(1000L);

            Long result = purchaseOrderService.createOrder(request, 1L);

            assertThat(result).isEqualTo(1000L);
            then(purchaseOrderProcessor).should(times(1)).createOnce(request, 1L);
        }

        @Test
        @DisplayName("발주번호 UNIQUE 충돌이 한 번 나면 새 트랜잭션으로 재시도하여 성공한다")
        void createOrder_numberCollisionOnce_retriesAndSucceeds() {
            PurchaseOrderCreateRequest request = createRequest();
            given(purchaseOrderProcessor.createOnce(any(), any()))
                    .willThrow(new DataIntegrityViolationException("발주번호 중복"))
                    .willReturn(1000L);

            Long result = purchaseOrderService.createOrder(request, 1L);

            assertThat(result).isEqualTo(1000L);
            then(purchaseOrderProcessor).should(times(2)).createOnce(request, 1L);
        }

        @Test
        @DisplayName("최대 재시도 횟수(3)를 모두 소진하면 CONCURRENCY_RETRY_EXHAUSTED로 실패한다")
        void createOrder_numberCollisionExhausted_throwsException() {
            PurchaseOrderCreateRequest request = createRequest();
            given(purchaseOrderProcessor.createOnce(any(), any()))
                    .willThrow(new DataIntegrityViolationException("계속 충돌"));

            assertThatThrownBy(() -> purchaseOrderService.createOrder(request, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.CONCURRENCY_RETRY_EXHAUSTED));

            then(purchaseOrderProcessor).should(times(3)).createOnce(request, 1L);
        }
    }

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

    @Nested
    @DisplayName("approveOrder")
    class ApproveOrder {

        @Test
        @DisplayName("매니저가 타인의 PENDING 발주를 승인하면 APPROVED 상태가 된다")
        void approveOrder_validRequest_changesStatusToApproved() {
            PurchaseOrder order = PurchaseOrder.create("PO-20260306-001", purchaseSupplier, requester, null);
            order.addItem(PurchaseOrderItem.create(product1, 5, BigDecimal.valueOf(10000)));

            given(purchaseOrderRepository.findById(1L)).willReturn(Optional.of(order));
            given(memberRepository.findById(2L)).willReturn(Optional.of(manager));

            purchaseOrderService.approveOrder(1L, 2L);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.APPROVED);
            assertThat(order.getApprover()).isEqualTo(manager);
            assertThat(order.getProcessedAt()).isNotNull();
        }

        @Test
        @DisplayName("요청자 본인이 승인하려고 하면 ORDER_SELF_APPROVAL 예외가 발생한다")
        void approveOrder_selfApproval_throwsException() {
            PurchaseOrder order = PurchaseOrder.create("PO-20260306-001", purchaseSupplier, requester, null);
            order.addItem(PurchaseOrderItem.create(product1, 5, BigDecimal.valueOf(10000)));

            given(purchaseOrderRepository.findById(1L)).willReturn(Optional.of(order));
            given(memberRepository.findById(1L)).willReturn(Optional.of(requester));

            assertThatThrownBy(() -> purchaseOrderService.approveOrder(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_SELF_APPROVAL));
        }
    }

    @Nested
    @DisplayName("rejectOrder")
    class RejectOrder {

        @Test
        @DisplayName("매니저가 타인의 PENDING 발주를 반려하면 REJECTED 상태가 되고 사유가 저장된다")
        void rejectOrder_validRequest_changesStatusToRejected() {
            PurchaseOrder order = PurchaseOrder.create("PO-20260306-001", purchaseSupplier, requester, null);
            order.addItem(PurchaseOrderItem.create(product1, 5, BigDecimal.valueOf(10000)));

            given(purchaseOrderRepository.findById(1L)).willReturn(Optional.of(order));
            given(memberRepository.findById(2L)).willReturn(Optional.of(manager));

            purchaseOrderService.rejectOrder(1L, 2L, "단가 재협상 필요");

            assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
            assertThat(order.getApprover()).isEqualTo(manager);
            assertThat(order.getRejectReason()).isEqualTo("단가 재협상 필요");
            assertThat(order.getProcessedAt()).isNotNull();
        }

        @Test
        @DisplayName("요청자 본인이 반려하려고 하면 ORDER_SELF_APPROVAL 예외가 발생한다")
        void rejectOrder_selfReject_throwsException() {
            PurchaseOrder order = PurchaseOrder.create("PO-20260306-001", purchaseSupplier, requester, null);
            order.addItem(PurchaseOrderItem.create(product1, 5, BigDecimal.valueOf(10000)));

            given(purchaseOrderRepository.findById(1L)).willReturn(Optional.of(order));
            given(memberRepository.findById(1L)).willReturn(Optional.of(requester));

            assertThatThrownBy(() -> purchaseOrderService.rejectOrder(1L, 1L, "사유"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_SELF_APPROVAL));
        }
    }

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        @Test
        @DisplayName("요청자 본인이 PENDING 발주를 취소하면 CANCELLED 상태가 된다")
        void cancelOrder_byRequester_changesStatusToCancelled() {
            PurchaseOrder order = PurchaseOrder.create("PO-20260306-001", purchaseSupplier, requester, null);
            order.addItem(PurchaseOrderItem.create(product1, 5, BigDecimal.valueOf(10000)));

            given(purchaseOrderRepository.findById(1L)).willReturn(Optional.of(order));

            purchaseOrderService.cancelOrder(1L, 1L);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("요청자가 아닌 사람이 취소하려고 하면 ORDER_NOT_REQUESTER 예외가 발생한다")
        void cancelOrder_notRequester_throwsException() {
            PurchaseOrder order = PurchaseOrder.create("PO-20260306-001", purchaseSupplier, requester, null);
            order.addItem(PurchaseOrderItem.create(product1, 5, BigDecimal.valueOf(10000)));

            given(purchaseOrderRepository.findById(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> purchaseOrderService.cancelOrder(1L, 2L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_NOT_REQUESTER));
        }
    }

    // 헬퍼 메서드

    private PurchaseOrderCreateRequest createRequest() {
        PurchaseOrderCreateRequest request = new PurchaseOrderCreateRequest();
        request.setSupplierId(1L);
        request.setNote(null);
        request.setItems(List.of());
        return request;
    }
}
