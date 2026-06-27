package com.stockandorder.domain.outbound.service;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import com.stockandorder.domain.outbound.dto.OutboundCreateRequest;
import com.stockandorder.domain.outbound.dto.OutboundListResponse;
import com.stockandorder.domain.outbound.dto.OutboundResponse;
import com.stockandorder.domain.outbound.dto.OutboundSearchCondition;
import com.stockandorder.domain.outbound.entity.Outbound;
import com.stockandorder.domain.outbound.entity.OutboundItem;
import com.stockandorder.domain.outbound.repository.OutboundRepository;
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
class OutboundServiceTest {

    @InjectMocks
    private OutboundService outboundService;

    @Mock
    private OutboundProcessor outboundProcessor;
    @Mock
    private OutboundRepository outboundRepository;

    private OutboundCreateRequest request;

    @BeforeEach
    void setUp() {
        request = new OutboundCreateRequest();
        request.setSupplierId(1L);
        request.setOutboundDate(LocalDate.now());
        request.setItems(List.of());
    }

    @Nested
    @DisplayName("createOutbound (재시도 정책)")
    class CreateOutbound {

        @Test
        @DisplayName("첫 시도에 성공하면 createOnce를 1회만 호출하고 id를 반환한다")
        void createOutbound_succeedsFirstTry() {
            given(outboundProcessor.createOnce(any(), any())).willReturn(1000L);

            Long result = outboundService.createOutbound(request, 5L);

            assertThat(result).isEqualTo(1000L);
            then(outboundProcessor).should(times(1)).createOnce(request, 5L);
        }

        @Test
        @DisplayName("출고번호 UNIQUE 충돌이 나면 재시도하여 성공한다")
        void createOutbound_retriesOnDataIntegrityViolation() {
            given(outboundProcessor.createOnce(any(), any()))
                    .willThrow(new DataIntegrityViolationException("출고번호 중복"))
                    .willReturn(1000L);

            Long result = outboundService.createOutbound(request, 5L);

            assertThat(result).isEqualTo(1000L);
            then(outboundProcessor).should(times(2)).createOnce(request, 5L);
        }

        @Test
        @DisplayName("최대 재시도 횟수(3)를 모두 소진하면 INTERNAL_SERVER_ERROR로 실패한다")
        void createOutbound_exhaustsRetries_throws() {
            given(outboundProcessor.createOnce(any(), any()))
                    .willThrow(new DataIntegrityViolationException("계속 충돌"));

            assertThatThrownBy(() -> outboundService.createOutbound(request, 5L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR));
            then(outboundProcessor).should(times(3)).createOnce(request, 5L);
        }
    }

    @Nested
    @DisplayName("searchOutbounds")
    class SearchOutbounds {

        @Test
        @DisplayName("condition과 pageable을 repository에 위임하고 결과를 그대로 반환한다")
        void searchOutbounds_delegatesToRepository() {
            OutboundSearchCondition condition = new OutboundSearchCondition();
            Pageable pageable = PageRequest.of(0, 10);
            OutboundListResponse listResponse = new OutboundListResponse(
                    1L, "OUT-20260622-001", "판매처A", "매니저1", BigDecimal.valueOf(79000), LocalDate.now());
            Page<OutboundListResponse> expected = new PageImpl<>(List.of(listResponse), pageable, 1);
            given(outboundRepository.search(condition, pageable)).willReturn(expected);

            Page<OutboundListResponse> result = outboundService.searchOutbounds(condition, pageable);

            assertThat(result).isSameAs(expected);
            then(outboundRepository).should().search(condition, pageable);
        }
    }

    @Nested
    @DisplayName("getOutbound")
    class GetOutbound {

        @Test
        @DisplayName("존재하는 출고 조회 시 OutboundResponse로 매핑하여 반환한다")
        void getOutbound_existing_returnsResponse() {
            given(outboundRepository.findDetailById(1L)).willReturn(Optional.of(sampleOutbound()));

            OutboundResponse result = outboundService.getOutbound(1L);

            assertThat(result.getOutboundNumber()).isEqualTo("OUT-20260622-001");
            assertThat(result.getSupplierName()).isEqualTo("판매처A");
            assertThat(result.getProcessorName()).isEqualTo("매니저1");
            assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(65000));
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getProductName()).isEqualTo("밀가루");
            assertThat(result.getItems().get(0).getQuantity()).isEqualTo(5);
            assertThat(result.getItems().get(0).getLineAmount()).isEqualByComparingTo(BigDecimal.valueOf(65000));
        }

        @Test
        @DisplayName("존재하지 않는 출고 조회 시 OUTBOUND_NOT_FOUND 예외가 발생한다")
        void getOutbound_notFound_throws() {
            given(outboundRepository.findDetailById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> outboundService.getOutbound(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                            .isEqualTo(ErrorCode.OUTBOUND_NOT_FOUND));
        }
    }

    private Outbound sampleOutbound() {
        Supplier supplier = Supplier.create("판매처A", SupplierType.SALES,
                "담당자", null, null, null);
        Member processor = Member.create("manager1", "password", "매니저1", "manager1@test.com", Role.MANAGER);

        Category category = Category.create("식자재", null);
        Product product = Product.create("PRD-001", "밀가루", category, "KG",
                BigDecimal.valueOf(10000), BigDecimal.valueOf(13000), 10, null);

        OutboundItem item = OutboundItem.create(product, 5, BigDecimal.valueOf(13000));
        Outbound outbound = Outbound.create("OUT-20260622-001", supplier, processor,
                LocalDate.now(), "테스트 출고", List.of(item));
        ReflectionTestUtils.setField(outbound, "outboundId", 1L);
        return outbound;
    }
}
