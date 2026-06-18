package com.stockandorder.domain.inbound.service;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.inbound.dto.InboundCreateRequest;
import com.stockandorder.domain.inbound.entity.Inbound;
import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import com.stockandorder.domain.member.repository.MemberRepository;
import com.stockandorder.domain.order.entity.PurchaseOrder;
import com.stockandorder.domain.order.entity.PurchaseOrderItem;
import com.stockandorder.domain.order.enums.OrderStatus;
import com.stockandorder.domain.order.repository.PurchaseOrderRepository;
import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.domain.stock.entity.Stock;
import com.stockandorder.domain.stock.entity.StockLog;
import com.stockandorder.domain.stock.enums.StockChangeType;
import com.stockandorder.domain.stock.repository.StockLogRepository;
import com.stockandorder.domain.stock.repository.StockRepository;
import com.stockandorder.domain.supplier.entity.Supplier;
import com.stockandorder.domain.supplier.enums.SupplierType;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class InboundProcessorTest {

    @InjectMocks
    private InboundProcessor inboundProcessor;

    @Mock
    private com.stockandorder.domain.inbound.repository.InboundRepository inboundRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private StockLogRepository stockLogRepository;

    private static final long ORDER_ID = 1L;
    private static final long PROCESSOR_ID = 5L;
    private static final long ORDER_ITEM_1_ID = 100L;
    private static final long ORDER_ITEM_2_ID = 200L;
    private static final long PRODUCT_1_ID = 10L;
    private static final long PRODUCT_2_ID = 20L;
    private static final long GENERATED_INBOUND_ID = 1000L;

    private PurchaseOrder order;
    private Member processor;
    private PurchaseOrderItem orderItem1;
    private PurchaseOrderItem orderItem2;
    private Stock stock1;
    private Stock stock2;

    @BeforeEach
    void setUp() {
        Supplier supplier = Supplier.create("공급처A", SupplierType.PURCHASE,
                "담당자", "010-1234-5678", "test@test.com", "서울");
        Member requester = Member.create("staff1", "password", "직원1", "staff1@test.com", Role.STAFF);
        ReflectionTestUtils.setField(requester, "memberId", 2L);

        processor = Member.create("manager1", "password", "매니저1", "manager1@test.com", Role.MANAGER);
        ReflectionTestUtils.setField(processor, "memberId", PROCESSOR_ID);

        Category category = Category.create("식자재", null);
        Product product1 = Product.create("PRD-001", "밀가루", category, "KG",
                BigDecimal.valueOf(10000), 10, null);
        ReflectionTestUtils.setField(product1, "productId", PRODUCT_1_ID);
        Product product2 = Product.create("PRD-002", "설탕", category, "KG",
                BigDecimal.valueOf(5000), 5, null);
        ReflectionTestUtils.setField(product2, "productId", PRODUCT_2_ID);

        order = PurchaseOrder.create("PO-20260610-001", supplier, requester, null);
        orderItem1 = PurchaseOrderItem.create(product1, 10, BigDecimal.valueOf(10000));
        orderItem2 = PurchaseOrderItem.create(product2, 20, BigDecimal.valueOf(5000));
        order.addItem(orderItem1);
        order.addItem(orderItem2);
        ReflectionTestUtils.setField(orderItem1, "orderItemId", ORDER_ITEM_1_ID);
        ReflectionTestUtils.setField(orderItem2, "orderItemId", ORDER_ITEM_2_ID);
        order.approve(processor);

        stock1 = Stock.create(product1);
        stock2 = Stock.create(product2);
    }

    // 입고 처리 시 InboundRepository.save가 호출되면 inboundId를 부여(실제 IDENTITY 채번 흉내)
    private void givenSaveAssignsId() {
        given(inboundRepository.save(any(Inbound.class))).willAnswer(invocation -> {
            Inbound inbound = invocation.getArgument(0);
            ReflectionTestUtils.setField(inbound, "inboundId", GENERATED_INBOUND_ID);
            return inbound;
        });
    }

    @Nested
    @DisplayName("createOnce 정상 처리")
    class HappyPath {

        @Test
        @DisplayName("전량 입고 시 재고 증가·이력 기록·발주 항목 누적이 일어나고 발주는 COMPLETED가 된다")
        void createOnce_fullReceipt_updatesStockAndLogsAndCompletesOrder() {
            given(purchaseOrderRepository.findForReceipt(ORDER_ID)).willReturn(Optional.of(order));
            given(memberRepository.findById(PROCESSOR_ID)).willReturn(Optional.of(processor));
            given(inboundRepository.findMaxInboundNumberByPrefix(anyString())).willReturn(Optional.empty());
            givenSaveAssignsId();
            given(stockRepository.findByProductIdForUpdate(PRODUCT_1_ID)).willReturn(Optional.of(stock1));
            given(stockRepository.findByProductIdForUpdate(PRODUCT_2_ID)).willReturn(Optional.of(stock2));

            InboundCreateRequest request = request(
                    item(ORDER_ITEM_1_ID, 10),
                    item(ORDER_ITEM_2_ID, 20));

            Long inboundId = inboundProcessor.createOnce(request, PROCESSOR_ID);

            assertThat(inboundId).isEqualTo(GENERATED_INBOUND_ID);
            // 재고 반영
            assertThat(stock1.getQuantity()).isEqualTo(10);
            assertThat(stock2.getQuantity()).isEqualTo(20);
            // 발주 항목 누적 입고량
            assertThat(orderItem1.getReceivedQuantity()).isEqualTo(10);
            assertThat(orderItem2.getReceivedQuantity()).isEqualTo(20);
            // 전량 입고 → 발주 COMPLETED
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        }

        @Test
        @DisplayName("부분 입고 시 발주는 IN_PROGRESS가 된다")
        void createOnce_partialReceipt_orderInProgress() {
            given(purchaseOrderRepository.findForReceipt(ORDER_ID)).willReturn(Optional.of(order));
            given(memberRepository.findById(PROCESSOR_ID)).willReturn(Optional.of(processor));
            given(inboundRepository.findMaxInboundNumberByPrefix(anyString())).willReturn(Optional.empty());
            givenSaveAssignsId();
            given(stockRepository.findByProductIdForUpdate(PRODUCT_1_ID)).willReturn(Optional.of(stock1));

            // 첫 항목만 일부 입고
            InboundCreateRequest request = request(item(ORDER_ITEM_1_ID, 4));

            inboundProcessor.createOnce(request, PROCESSOR_ID);

            assertThat(orderItem1.getReceivedQuantity()).isEqualTo(4);
            assertThat(stock1.getQuantity()).isEqualTo(4);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("기존 입고번호가 없으면 IN-yyyyMMdd-001 형식으로 채번한다")
        void createOnce_generatesInboundNumber() {
            given(purchaseOrderRepository.findForReceipt(ORDER_ID)).willReturn(Optional.of(order));
            given(memberRepository.findById(PROCESSOR_ID)).willReturn(Optional.of(processor));
            given(inboundRepository.findMaxInboundNumberByPrefix(anyString())).willReturn(Optional.empty());
            givenSaveAssignsId();
            given(stockRepository.findByProductIdForUpdate(PRODUCT_1_ID)).willReturn(Optional.of(stock1));

            inboundProcessor.createOnce(request(item(ORDER_ITEM_1_ID, 1)), PROCESSOR_ID);

            ArgumentCaptor<Inbound> captor = ArgumentCaptor.forClass(Inbound.class);
            then(inboundRepository).should().save(captor.capture());
            String expectedPrefix = "IN-" + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            assertThat(captor.getValue().getInboundNumber())
                    .startsWith(expectedPrefix)
                    .endsWith("-001");
        }

        @Test
        @DisplayName("기존 입고번호가 있으면 다음 일련번호로 채번한다")
        void createOnce_incrementsInboundNumber() {
            String prefix = "IN-" + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
            given(purchaseOrderRepository.findForReceipt(ORDER_ID)).willReturn(Optional.of(order));
            given(memberRepository.findById(PROCESSOR_ID)).willReturn(Optional.of(processor));
            given(inboundRepository.findMaxInboundNumberByPrefix(anyString()))
                    .willReturn(Optional.of(prefix + "007"));
            givenSaveAssignsId();
            given(stockRepository.findByProductIdForUpdate(PRODUCT_1_ID)).willReturn(Optional.of(stock1));

            inboundProcessor.createOnce(request(item(ORDER_ITEM_1_ID, 1)), PROCESSOR_ID);

            ArgumentCaptor<Inbound> captor = ArgumentCaptor.forClass(Inbound.class);
            then(inboundRepository).should().save(captor.capture());
            assertThat(captor.getValue().getInboundNumber()).isEqualTo(prefix + "008");
        }

        @Test
        @DisplayName("재고 변동 이력(StockLog)이 INBOUND 타입으로 inbound_id를 참조하여 기록된다")
        void createOnce_recordsStockLog() {
            given(purchaseOrderRepository.findForReceipt(ORDER_ID)).willReturn(Optional.of(order));
            given(memberRepository.findById(PROCESSOR_ID)).willReturn(Optional.of(processor));
            given(inboundRepository.findMaxInboundNumberByPrefix(anyString())).willReturn(Optional.empty());
            givenSaveAssignsId();
            given(stockRepository.findByProductIdForUpdate(PRODUCT_1_ID)).willReturn(Optional.of(stock1));

            inboundProcessor.createOnce(request(item(ORDER_ITEM_1_ID, 6)), PROCESSOR_ID);

            ArgumentCaptor<StockLog> captor = ArgumentCaptor.forClass(StockLog.class);
            then(stockLogRepository).should().save(captor.capture());
            StockLog log = captor.getValue();
            assertThat(log.getChangeType()).isEqualTo(StockChangeType.INBOUND);
            assertThat(log.getChangeQuantity()).isEqualTo(6);
            assertThat(log.getBeforeQuantity()).isZero();
            assertThat(log.getAfterQuantity()).isEqualTo(6);
            assertThat(log.getReferenceId()).isEqualTo(GENERATED_INBOUND_ID);
        }
    }

    @Nested
    @DisplayName("createOnce 검증 실패")
    class Validation {

        @Test
        @DisplayName("발주가 없으면 ORDER_NOT_FOUND 예외가 발생한다")
        void createOnce_orderNotFound_throws() {
            given(purchaseOrderRepository.findForReceipt(ORDER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> inboundProcessor.createOnce(request(item(ORDER_ITEM_1_ID, 1)), PROCESSOR_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_NOT_FOUND));
            then(inboundRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("입고 불가 상태(PENDING) 발주면 INBOUND_ORDER_NOT_APPROVED 예외가 발생한다")
        void createOnce_orderNotReceivable_throws() {
            PurchaseOrder pending = PurchaseOrder.create("PO-20260610-002", order.getSupplier(),
                    order.getRequester(), null);
            pending.addItem(orderItem1);
            given(purchaseOrderRepository.findForReceipt(ORDER_ID)).willReturn(Optional.of(pending));

            assertThatThrownBy(() -> inboundProcessor.createOnce(request(item(ORDER_ITEM_1_ID, 1)), PROCESSOR_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INBOUND_ORDER_NOT_APPROVED));
            then(inboundRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("처리자(Member)가 없으면 MEMBER_NOT_FOUND 예외가 발생한다")
        void createOnce_processorNotFound_throws() {
            given(purchaseOrderRepository.findForReceipt(ORDER_ID)).willReturn(Optional.of(order));
            given(memberRepository.findById(PROCESSOR_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> inboundProcessor.createOnce(request(item(ORDER_ITEM_1_ID, 1)), PROCESSOR_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
            then(inboundRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("이 발주에 속하지 않은 발주 항목을 가리키면 ORDER_ITEM_NOT_FOUND 예외가 발생한다")
        void createOnce_orderItemNotFound_throws() {
            given(purchaseOrderRepository.findForReceipt(ORDER_ID)).willReturn(Optional.of(order));
            given(memberRepository.findById(PROCESSOR_ID)).willReturn(Optional.of(processor));

            assertThatThrownBy(() -> inboundProcessor.createOnce(request(item(999L, 1)), PROCESSOR_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ORDER_ITEM_NOT_FOUND));
            then(inboundRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("발주 잔여 수량을 초과 입고하면 INBOUND_QUANTITY_EXCEEDED 예외가 발생한다")
        void createOnce_quantityExceeded_throws() {
            given(purchaseOrderRepository.findForReceipt(ORDER_ID)).willReturn(Optional.of(order));
            given(memberRepository.findById(PROCESSOR_ID)).willReturn(Optional.of(processor));
            given(inboundRepository.findMaxInboundNumberByPrefix(anyString())).willReturn(Optional.empty());
            givenSaveAssignsId();

            // orderItem1의 발주 수량은 10인데 11 입고 시도
            assertThatThrownBy(() -> inboundProcessor.createOnce(request(item(ORDER_ITEM_1_ID, 11)), PROCESSOR_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INBOUND_QUANTITY_EXCEEDED));
        }

        @Test
        @DisplayName("상품의 재고 레코드가 없으면 STOCK_NOT_FOUND 예외가 발생한다")
        void createOnce_stockNotFound_throws() {
            given(purchaseOrderRepository.findForReceipt(ORDER_ID)).willReturn(Optional.of(order));
            given(memberRepository.findById(PROCESSOR_ID)).willReturn(Optional.of(processor));
            given(inboundRepository.findMaxInboundNumberByPrefix(anyString())).willReturn(Optional.empty());
            givenSaveAssignsId();
            given(stockRepository.findByProductIdForUpdate(PRODUCT_1_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> inboundProcessor.createOnce(request(item(ORDER_ITEM_1_ID, 1)), PROCESSOR_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.STOCK_NOT_FOUND));
        }
    }

    // 헬퍼

    private InboundCreateRequest request(InboundCreateRequest.ItemRequest... items) {
        InboundCreateRequest request = new InboundCreateRequest();
        request.setOrderId(ORDER_ID);
        request.setInboundDate(LocalDate.now());
        request.setItems(List.of(items));
        return request;
    }

    private InboundCreateRequest.ItemRequest item(Long orderItemId, int quantity) {
        InboundCreateRequest.ItemRequest item = new InboundCreateRequest.ItemRequest();
        item.setOrderItemId(orderItemId);
        item.setQuantity(quantity);
        return item;
    }
}
