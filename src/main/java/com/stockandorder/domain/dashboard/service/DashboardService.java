package com.stockandorder.domain.dashboard.service;

import com.stockandorder.domain.dashboard.dto.DashboardResponse;
import com.stockandorder.domain.inbound.service.InboundService;
import com.stockandorder.domain.order.service.PurchaseOrderService;
import com.stockandorder.domain.outbound.service.OutboundService;
import com.stockandorder.domain.stock.dto.StockListResponse;
import com.stockandorder.domain.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

/**
 * 여러 도메인의 요약을 한 화면용으로 모으는 책임만 갖는다(SRP). 컨트롤러가 도메인마다 직접
 * 손을 뻗는 대신 여기서 조율한다.
 *
 * 각 도메인의 리포지토리를 직접 읽지 않고 도메인 서비스를 거친다 — 리포지토리는 그 도메인의
 * 내부 도구이고, "오늘을 어떻게 세는가" 같은 규칙도 각 도메인 안에 머물러야 하기 때문이다.
 *
 * 트랜잭션을 따로 두지 않는 이유: 위젯들은 서로 독립적인 읽기라 한 트랜잭션으로 묶을 정합성
 * 요구가 없다. 각 도메인 서비스 메서드가 자기 readOnly 트랜잭션을 갖는다.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    // 대시보드 경고 위젯에 보여줄 상위 건수
    private static final int LOW_STOCK_PREVIEW_LIMIT = 5;

    private final StockService stockService;
    private final InboundService inboundService;
    private final OutboundService outboundService;
    private final PurchaseOrderService purchaseOrderService;

    public DashboardResponse getDashboard() {
        // 경고 건수(getTotalElements)와 상위 N개(getContent)가 한 Page에 함께 담겨 온다.
        Page<StockListResponse> lowStock = stockService.getLowStockPreview(LOW_STOCK_PREVIEW_LIMIT);

        return new DashboardResponse(
                inboundService.countToday(),
                outboundService.countToday(),
                purchaseOrderService.countPending(),
                lowStock.getTotalElements(),
                lowStock.getContent());
    }
}
