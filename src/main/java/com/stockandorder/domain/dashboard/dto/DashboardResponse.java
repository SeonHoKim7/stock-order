package com.stockandorder.domain.dashboard.dto;

import com.stockandorder.domain.stock.dto.StockListResponse;
import lombok.Getter;

import java.util.List;

/**
 * 대시보드 한 화면에 필요한 여러 도메인의 요약을 하나로 묶은 응답.
 * 컨트롤러가 위젯마다 model attribute를 흩뿌리는 대신 이 객체 하나만 넘겨,
 * 화면이 기대하는 데이터의 모양을 한곳에서 드러낸다.
 *
 * 안전 재고 경고는 Page 대신 건수(lowStockCount)와 미리보기 목록(lowStockPreview)으로 펼쳐 담는다
 * — 화면이 Page API에 의존하지 않도록.
 */
@Getter
public class DashboardResponse {

    private final long todayInboundCount;
    private final long todayOutboundCount;
    private final long pendingOrderCount;
    private final long lowStockCount;
    private final List<StockListResponse> lowStockPreview;

    public DashboardResponse(long todayInboundCount, long todayOutboundCount, long pendingOrderCount,
                             long lowStockCount, List<StockListResponse> lowStockPreview) {
        this.todayInboundCount = todayInboundCount;
        this.todayOutboundCount = todayOutboundCount;
        this.pendingOrderCount = pendingOrderCount;
        this.lowStockCount = lowStockCount;
        this.lowStockPreview = lowStockPreview;
    }
}
