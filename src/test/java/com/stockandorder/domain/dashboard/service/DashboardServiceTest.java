package com.stockandorder.domain.dashboard.service;

import com.stockandorder.domain.dashboard.dto.DashboardResponse;
import com.stockandorder.domain.inbound.service.InboundService;
import com.stockandorder.domain.order.service.PurchaseOrderService;
import com.stockandorder.domain.outbound.service.OutboundService;
import com.stockandorder.domain.stock.dto.StockListResponse;
import com.stockandorder.domain.stock.service.StockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @InjectMocks
    private DashboardService dashboardService;

    @Mock
    private StockService stockService;
    @Mock
    private InboundService inboundService;
    @Mock
    private OutboundService outboundService;
    @Mock
    private PurchaseOrderService purchaseOrderService;

    @Test
    @DisplayName("각 도메인 서비스에서 위젯 값을 모아 DashboardResponse로 매핑한다")
    void getDashboard_aggregatesAllWidgets() {
        // given: 경고는 상위 2개만 미리보기로 오지만 전체 경고 건수(totalElements)는 7건이다.
        List<StockListResponse> preview = List.of(
                new StockListResponse(1L, "P-1", "상품1", "식자재", 0, 10),
                new StockListResponse(2L, "P-2", "상품2", "식자재", 3, 10));
        Page<StockListResponse> lowStockPage = new PageImpl<>(preview, PageRequest.of(0, 5), 7);

        given(inboundService.countToday()).willReturn(4L);
        given(outboundService.countToday()).willReturn(2L);
        given(purchaseOrderService.countPending()).willReturn(3L);
        given(stockService.getLowStockPreview(5)).willReturn(lowStockPage);

        // when
        DashboardResponse result = dashboardService.getDashboard();

        // then
        assertThat(result.getTodayInboundCount()).isEqualTo(4L);
        assertThat(result.getTodayOutboundCount()).isEqualTo(2L);
        assertThat(result.getPendingOrderCount()).isEqualTo(3L);
        // 경고 건수는 미리보기 개수가 아니라 전체 건수(totalElements)여야 한다.
        assertThat(result.getLowStockCount()).isEqualTo(7L);
        assertThat(result.getLowStockPreview()).hasSize(2);

        // 미리보기 상위 건수(5)를 그대로 위임하는지 고정한다.
        then(stockService).should().getLowStockPreview(5);
    }
}
