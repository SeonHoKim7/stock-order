package com.stockandorder.domain.outbound.service;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import com.stockandorder.domain.member.repository.MemberRepository;
import com.stockandorder.domain.outbound.dto.OutboundCreateRequest;
import com.stockandorder.domain.outbound.entity.Outbound;
import com.stockandorder.domain.outbound.repository.OutboundRepository;
import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.domain.product.repository.ProductRepository;
import com.stockandorder.domain.stock.service.StockService;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OutboundProcessorTest {

    @InjectMocks
    private OutboundProcessor outboundProcessor;

    @Mock
    private OutboundRepository outboundRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private StockService stockService;

    private static final long SUPPLIER_ID = 1L;
    private static final long PROCESSOR_ID = 5L;
    private static final long PRODUCT_1_ID = 10L;
    private static final long PRODUCT_2_ID = 20L;
    private static final long GENERATED_OUTBOUND_ID = 1000L;

    private Supplier supplier;
    private Member processor;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        supplier = Supplier.create("판매처A", SupplierType.SALES,
                "담당", "010-1234-5678", "test@test.com", "서울");
        ReflectionTestUtils.setField(supplier, "supplierId", SUPPLIER_ID);

        processor = Member.create("manager1", "password", "매니저1", "manager1@test.com", Role.MANAGER);
        ReflectionTestUtils.setField(processor, "memberId", PROCESSOR_ID);

        Category category = Category.create("식자재", null);
        product1 = Product.create("PRD-001", "밀가루", category, "KG",
                BigDecimal.valueOf(10000), BigDecimal.valueOf(13000), 10, null);
        ReflectionTestUtils.setField(product1, "productId", PRODUCT_1_ID);
        product2 = Product.create("PRD-002", "설탕", category, "KG",
                BigDecimal.valueOf(5000), BigDecimal.valueOf(7000), 5, null);
        ReflectionTestUtils.setField(product2, "productId", PRODUCT_2_ID);
    }

    private void givenSaveAssignsId() {
        given(outboundRepository.save(any(Outbound.class))).willAnswer(invocation -> {
            Outbound outbound = invocation.getArgument(0);
            ReflectionTestUtils.setField(outbound, "outboundId", GENERATED_OUTBOUND_ID);
            return outbound;
        });
    }

    private void givenCommonStubs() {
        given(supplierRepository.findById(SUPPLIER_ID)).willReturn(Optional.of(supplier));
        given(memberRepository.findById(PROCESSOR_ID)).willReturn(Optional.of(processor));
        lenient().when(productRepository.findById(PRODUCT_1_ID)).thenReturn(Optional.of(product1));
        lenient().when(productRepository.findById(PRODUCT_2_ID)).thenReturn(Optional.of(product2));
        given(outboundRepository.findMaxOutboundNumberByPrefix(anyString())).willReturn(Optional.empty());
        givenSaveAssignsId();
    }

    @Nested
    @DisplayName("createOnce 정상 처리")
    class HappyPath {

        @Test
        @DisplayName("출고 생성 시 재고 차감을 위임하고 매출 총액이 항목 합으로 계산된다")
        void createOnce_decreasesStockAndComputesTotal() {
            givenCommonStubs();

            OutboundCreateRequest request = request(
                    item(PRODUCT_1_ID, 5),
                    item(PRODUCT_2_ID, 2));

            Long outboundId = outboundProcessor.createOnce(request, PROCESSOR_ID);

            assertThat(outboundId).isEqualTo(GENERATED_OUTBOUND_ID);
            // 재고 차감은 StockService에 위임(referenceId = 방금 채번된 outbound_id)
            then(stockService).should().decrease(PRODUCT_1_ID, 5, GENERATED_OUTBOUND_ID);
            then(stockService).should().decrease(PRODUCT_2_ID, 2, GENERATED_OUTBOUND_ID);

            ArgumentCaptor<Outbound> captor = ArgumentCaptor.forClass(Outbound.class);
            then(outboundRepository).should().save(captor.capture());
            // 매출가 스냅샷 합: 13,000 * 5 + 7,000 * 2 = 79,000
            assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(79000));
        }

        @Test
        @DisplayName("데드락 방지를 위해 재고 차감은 productId 오름차순으로 호출된다")
        void createOnce_callsStockServiceInProductIdOrder() {
            givenCommonStubs();

            // 요청은 역순(2 → 1)으로 들어와도 호출은 오름차순(1 → 2)이어야 한다
            OutboundCreateRequest request = request(
                    item(PRODUCT_2_ID, 2),
                    item(PRODUCT_1_ID, 5));

            outboundProcessor.createOnce(request, PROCESSOR_ID);

            InOrder ordered = inOrder(stockService);
            ordered.verify(stockService).decrease(PRODUCT_1_ID, 5, GENERATED_OUTBOUND_ID);
            ordered.verify(stockService).decrease(PRODUCT_2_ID, 2, GENERATED_OUTBOUND_ID);
        }

        @Test
        @DisplayName("기존 출고번호가 없으면 OUT-yyyyMMdd-001 형식으로 채번한다")
        void createOnce_generatesOutboundNumber() {
            givenCommonStubs();

            outboundProcessor.createOnce(request(item(PRODUCT_1_ID, 1)), PROCESSOR_ID);

            ArgumentCaptor<Outbound> captor = ArgumentCaptor.forClass(Outbound.class);
            then(outboundRepository).should().save(captor.capture());
            String expectedPrefix = "OUT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            assertThat(captor.getValue().getOutboundNumber())
                    .startsWith(expectedPrefix)
                    .endsWith("-001");
        }

        @Test
        @DisplayName("기존 출고번호가 있으면 다음 일련번호로 채번한다")
        void createOnce_incrementsOutboundNumber() {
            String prefix = "OUT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
            given(supplierRepository.findById(SUPPLIER_ID)).willReturn(Optional.of(supplier));
            given(memberRepository.findById(PROCESSOR_ID)).willReturn(Optional.of(processor));
            given(productRepository.findById(PRODUCT_1_ID)).willReturn(Optional.of(product1));
            given(outboundRepository.findMaxOutboundNumberByPrefix(anyString()))
                    .willReturn(Optional.of(prefix + "007"));
            givenSaveAssignsId();

            outboundProcessor.createOnce(request(item(PRODUCT_1_ID, 1)), PROCESSOR_ID);

            ArgumentCaptor<Outbound> captor = ArgumentCaptor.forClass(Outbound.class);
            then(outboundRepository).should().save(captor.capture());
            assertThat(captor.getValue().getOutboundNumber()).isEqualTo(prefix + "008");
        }
    }

    @Nested
    @DisplayName("createOnce 검증 실패")
    class Validation {

        @Test
        @DisplayName("판매처가 없으면 SUPPLIER_NOT_FOUND 예외가 발생한다")
        void createOnce_supplierNotFound_throws() {
            given(supplierRepository.findById(SUPPLIER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> outboundProcessor.createOnce(request(item(PRODUCT_1_ID, 1)), PROCESSOR_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SUPPLIER_NOT_FOUND));
            then(outboundRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("비활성 판매처면 SUPPLIER_INACTIVE 예외가 발생한다")
        void createOnce_supplierInactive_throws() {
            supplier.deactivate();
            given(supplierRepository.findById(SUPPLIER_ID)).willReturn(Optional.of(supplier));

            assertThatThrownBy(() -> outboundProcessor.createOnce(request(item(PRODUCT_1_ID, 1)), PROCESSOR_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SUPPLIER_INACTIVE));
            then(outboundRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("A-4: 매입 전용(PURCHASE) 거래처로 출고하면 SUPPLIER_TYPE_INVALID 예외가 발생한다")
        void createOnce_purchaseTypeSupplier_throws() {
            Supplier purchaseOnly = Supplier.create("공급처B", SupplierType.PURCHASE,
                    "담당", "010-0000-0000", "b@b.com", "서울");
            ReflectionTestUtils.setField(purchaseOnly, "supplierId", SUPPLIER_ID);
            given(supplierRepository.findById(SUPPLIER_ID)).willReturn(Optional.of(purchaseOnly));

            assertThatThrownBy(() -> outboundProcessor.createOnce(request(item(PRODUCT_1_ID, 1)), PROCESSOR_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.SUPPLIER_TYPE_INVALID));
            then(outboundRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("처리자(Member)가 없으면 MEMBER_NOT_FOUND 예외가 발생한다")
        void createOnce_processorNotFound_throws() {
            given(supplierRepository.findById(SUPPLIER_ID)).willReturn(Optional.of(supplier));
            given(memberRepository.findById(PROCESSOR_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> outboundProcessor.createOnce(request(item(PRODUCT_1_ID, 1)), PROCESSOR_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
            then(outboundRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("5: 한 출고서에 같은 상품을 중복으로 적으면 OUTBOUND_DUPLICATE_PRODUCT 예외가 발생한다")
        void createOnce_duplicateProduct_throws() {
            given(supplierRepository.findById(SUPPLIER_ID)).willReturn(Optional.of(supplier));
            given(memberRepository.findById(PROCESSOR_ID)).willReturn(Optional.of(processor));

            OutboundCreateRequest request = request(
                    item(PRODUCT_1_ID, 3),
                    item(PRODUCT_1_ID, 4)); // 같은 상품을 두 줄로

            assertThatThrownBy(() -> outboundProcessor.createOnce(request, PROCESSOR_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.OUTBOUND_DUPLICATE_PRODUCT));
            then(outboundRepository).should(never()).save(any());
            then(stockService).should(never()).decrease(anyLong(), anyInt(), anyLong());
        }

        @Test
        @DisplayName("존재하지 않는 상품을 가리키면 PRODUCT_NOT_FOUND 예외가 발생한다")
        void createOnce_productNotFound_throws() {
            given(supplierRepository.findById(SUPPLIER_ID)).willReturn(Optional.of(supplier));
            given(memberRepository.findById(PROCESSOR_ID)).willReturn(Optional.of(processor));
            given(productRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> outboundProcessor.createOnce(request(item(999L, 1)), PROCESSOR_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND));
            then(outboundRepository).should(never()).save(any());
        }
    }

    // 헬퍼

    private OutboundCreateRequest request(OutboundCreateRequest.ItemRequest... items) {
        OutboundCreateRequest request = new OutboundCreateRequest();
        request.setSupplierId(SUPPLIER_ID);
        request.setOutboundDate(LocalDate.now());
        request.setItems(List.of(items));
        return request;
    }

    private OutboundCreateRequest.ItemRequest item(Long productId, int quantity) {
        OutboundCreateRequest.ItemRequest item = new OutboundCreateRequest.ItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }
}
