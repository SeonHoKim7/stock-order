package com.stockandorder.domain.inbound.service;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.inbound.dto.InboundCreateRequest;
import com.stockandorder.domain.inbound.dto.InboundListResponse;
import com.stockandorder.domain.inbound.dto.InboundResponse;
import com.stockandorder.domain.inbound.dto.InboundSearchCondition;
import com.stockandorder.domain.inbound.entity.Inbound;
import com.stockandorder.domain.inbound.entity.InboundItem;
import com.stockandorder.domain.inbound.repository.InboundRepository;
import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import com.stockandorder.domain.order.entity.PurchaseOrder;
import com.stockandorder.domain.order.entity.PurchaseOrderItem;
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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class InboundServiceTest {

    @InjectMocks
    private InboundService inboundService;

    @Mock
    private InboundProcessor inboundProcessor;
    @Mock
    private InboundRepository inboundRepository;

    private InboundCreateRequest request;

    @BeforeEach
    void setUp() {
        request = new InboundCreateRequest();
        request.setOrderId(1L);
        request.setInboundDate(LocalDate.now());
        request.setItems(List.of());
    }

    @Nested
    @DisplayName("createInbound (재시도 정책)")
    class CreateInbound {

        @Test
        @DisplayName("첫 시도에 성공하면 createOnce를 1회만 호출하고 id를 반환한다")
        void createInbound_succeedsFirstTry() {
            given(inboundProcessor.createOnce(any(), any())).willReturn(1000L);

            Long result = inboundService.createInbound(request, 5L);

            assertThat(result).isEqualTo(1000L);
            then(inboundProcessor).should(times(1)).createOnce(request, 5L);
        }

        @Test
        @DisplayName("낙관적 락 충돌이 한 번 나면 재시도하여 성공한다")
        void createInbound_retriesOnOptimisticLockingFailure() {
            given(inboundProcessor.createOnce(any(), any()))
                    .willThrow(new OptimisticLockingFailureException("발주 version 충돌"))
                    .willReturn(1000L);

            Long result = inboundService.createInbound(request, 5L);

            assertThat(result).isEqualTo(1000L);
            then(inboundProcessor).should(times(2)).createOnce(request, 5L);
        }

        @Test
        @DisplayName("입고번호 UNIQUE 충돌이 나면 재시도하여 성공한다")
        void createInbound_retriesOnDataIntegrityViolation() {
            given(inboundProcessor.createOnce(any(), any()))
                    .willThrow(new DataIntegrityViolationException("입고번호 중복"))
                    .willReturn(1000L);

            Long result = inboundService.createInbound(request, 5L);

            assertThat(result).isEqualTo(1000L);
            then(inboundProcessor).should(times(2)).createOnce(request, 5L);
        }

        @Test
        @DisplayName("최대 재시도 횟수(3)를 모두 소진하면 INTERNAL_SERVER_ERROR로 실패한다")
        void createInbound_exhaustsRetries_throws() {
            given(inboundProcessor.createOnce(any(), any()))
                    .willThrow(new OptimisticLockingFailureException("계속 충돌"));

            assertThatThrownBy(() -> inboundService.createInbound(request, 5L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR));
            then(inboundProcessor).should(times(3)).createOnce(request, 5L);
        }
    }

    @Nested
    @DisplayName("searchInbounds")
    class SearchInbounds {

        @Test
        @DisplayName("condition과 pageable을 repository에 위임하고 결과를 그대로 반환한다")
        void searchInbounds_delegatesToRepository() {
            InboundSearchCondition condition = new InboundSearchCondition();
            Pageable pageable = PageRequest.of(0, 10);
            InboundListResponse listResponse = new InboundListResponse(
                    1L, "IN-20260610-001", "PO-20260610-001", "공급처A", "매니저1", LocalDate.now());
            Page<InboundListResponse> expected = new PageImpl<>(List.of(listResponse), pageable, 1);
            given(inboundRepository.search(condition, pageable)).willReturn(expected);

            Page<InboundListResponse> result = inboundService.searchInbounds(condition, pageable);

            assertThat(result).isSameAs(expected);
            then(inboundRepository).should().search(condition, pageable);
        }
    }

    @Nested
    @DisplayName("getInbound")
    class GetInbound {

        @Test
        @DisplayName("존재하는 입고 조회 시 InboundResponse로 매핑하여 반환한다")
        void getInbound_existing_returnsResponse() {
            given(inboundRepository.findDetailById(1L)).willReturn(Optional.of(sampleInbound()));

            InboundResponse result = inboundService.getInbound(1L);

            assertThat(result.getInboundNumber()).isEqualTo("IN-20260610-001");
            assertThat(result.getOrderNumber()).isEqualTo("PO-20260610-001");
            assertThat(result.getSupplierName()).isEqualTo("공급처A");
            assertThat(result.getProcessorName()).isEqualTo("매니저1");
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getProductName()).isEqualTo("밀가루");
            assertThat(result.getItems().get(0).getQuantity()).isEqualTo(6);
            assertThat(result.getItems().get(0).getOrderedQuantity()).isEqualTo(10);
            assertThat(result.getItems().get(0).getReceivedQuantity()).isEqualTo(6);
        }

        @Test
        @DisplayName("존재하지 않는 입고 조회 시 INBOUND_NOT_FOUND 예외가 발생한다")
        void getInbound_notFound_throws() {
            given(inboundRepository.findDetailById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> inboundService.getInbound(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INBOUND_NOT_FOUND));
        }
    }

    private Inbound sampleInbound() {
        Supplier supplier = Supplier.create("공급처A", SupplierType.PURCHASE,
                "담당자", null, null, null);
        Member requester = Member.create("staff1", "password", "직원1", "staff1@test.com", Role.STAFF);
        Member processor = Member.create("manager1", "password", "매니저1", "manager1@test.com", Role.MANAGER);

        Category category = Category.create("식자재", null);
        Product product = Product.create("PRD-001", "밀가루", category, "KG",
                BigDecimal.valueOf(10000), 10, null);

        PurchaseOrder order = PurchaseOrder.create("PO-20260610-001", supplier, requester, null);
        PurchaseOrderItem orderItem = PurchaseOrderItem.create(product, 10, BigDecimal.valueOf(10000));
        order.addItem(orderItem);
        ReflectionTestUtils.setField(orderItem, "orderItemId", 100L);
        order.approve(processor);
        orderItem.receive(6);

        Inbound inbound = Inbound.create("IN-20260610-001", order, processor,
                LocalDate.now(), "테스트 입고", List.of(InboundItem.create(orderItem, 6)));
        ReflectionTestUtils.setField(inbound, "inboundId", 1L);
        return inbound;
    }
}
