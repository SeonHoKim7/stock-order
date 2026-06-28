package com.stockandorder.domain.stock.service;

import com.stockandorder.domain.stock.dto.StockListResponse;
import com.stockandorder.domain.stock.dto.StockSearchCondition;
import com.stockandorder.domain.stock.entity.Stock;
import com.stockandorder.domain.stock.entity.StockLog;
import com.stockandorder.domain.stock.enums.StockChangeType;
import com.stockandorder.domain.stock.enums.StockStatus;
import com.stockandorder.domain.stock.repository.StockLogRepository;
import com.stockandorder.domain.stock.repository.StockRepository;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 재고 변경의 단일 통로(G-1). "비관적 락 획득 → 재고 증감 → before/after 스냅샷 → StockLog 기록"을
 * 하나의 원자 단위로 캡슐화한다.
 *
 * 이렇게 모으는 이유:
 * - I-1/I-2: 재고를 바꾸면 변동 이력(append-only)이 반드시 함께 남는다. 호출자(입고/출고/조정)가
 *   로그 기록을 빼먹는 것이 구조적으로 불가능하다.
 * - 출고(decrease)·재고조정(adjust)도 같은 "변경+로그+스냅샷" 패턴이므로 여기서 재사용한다(DRY).
 *
 * 트랜잭션/락 주의:
 * - 재고 변경과 로그 기록은 같은 트랜잭션이어야 하므로, 이 메서드는 트랜잭션을 가진 오케스트레이터
 *   (예: InboundProcessor.createOnce) 안에서 호출되어야 한다.
 * - 비관적 락을 이 메서드 안에서 획득하므로, 한 트랜잭션에서 여러 상품을 처리할 때 데드락을 막으려면
 *   호출 측이 productId 오름차순으로 호출해 락 획득 순서를 고정해야 한다.
 */
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final StockLogRepository stockLogRepository;

    /**
     * 재고 현황 목록 조회(읽기 전용). 변경 경로와 달리 락을 잡지 않는다(찰나의 stale 허용).
     */
    @Transactional(readOnly = true)
    public Page<StockListResponse> searchStocks(StockSearchCondition condition, Pageable pageable) {
        return stockRepository.search(condition, pageable);
    }

    /**
     * 안전 재고 경고 미리보기(대시보드 위젯용). "경고 = 품절+미달, 위험도순"이라는 정의를 이 한 곳에 모은다.
     * 별도 쿼리를 만들지 않고 search()를 재사용한다 — 반환된 Page 한 개에 경고 건수(getTotalElements)와
     * 상위 N개(getContent)가 함께 담겨 대시보드가 필요한 모양과 정확히 일치한다.
     *
     * @param limit 미리보기로 보여줄 상위 건수
     */
    @Transactional(readOnly = true)
    public Page<StockListResponse> getLowStockPreview(int limit) {
        StockSearchCondition condition = new StockSearchCondition();
        condition.setStatuses(List.of(StockStatus.OUT_OF_STOCK, StockStatus.SHORTAGE));
        condition.setSort("RISK");
        return stockRepository.search(condition, PageRequest.of(0, limit));
    }

    /**
     * 입고에 따른 재고 증가 + 변동 이력 기록.
     *
     * @param productId   대상 상품
     * @param quantity    증가 수량(양수)
     * @param referenceId 변동 원본(입고 id). StockLog.referenceId에 기록되어 추적에 쓰인다.
     */
    public void increase(Long productId, int quantity, Long referenceId) {
        Stock stock = stockRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));

        int before = stock.getQuantity();
        stock.increase(quantity);
        int after = stock.getQuantity();

        // I-2/I-3: 변동 결과를 before/after 스냅샷과 함께 append-only 로그로 남긴다.
        // 입고는 시스템 자동 변동이라 referenceId(입고 id)로 추적되므로 reason은 null(I-4).
        StockLog log = StockLog.of(stock.getProduct(), StockChangeType.INBOUND,
                quantity, before, after, referenceId, null);
        stockLogRepository.save(log);
    }

    /**
     * 출고에 따른 재고 감소 + 변동 이력 기록. increase()의 거울이다.
     * 재고 부족이면 Stock.decrease()가 STOCK_INSUFFICIENT를 던져 음수 재고를 구조적으로 막는다.
     *
     * @param productId   대상 상품
     * @param quantity    감소 수량(양수). StockLog에는 음수(-quantity)로 기록된다.
     * @param referenceId 변동 원본(출고 id). StockLog.referenceId에 기록되어 추적에 쓰인다.
     */
    public void decrease(Long productId, int quantity, Long referenceId) {
        Stock stock = stockRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));

        int before = stock.getQuantity();
        stock.decrease(quantity);
        int after = stock.getQuantity();

        // OUTBOUND 로그는 변동량을 음수로 기록한다(StockLog가 방향을 검증함).
        StockLog log = StockLog.of(stock.getProduct(), StockChangeType.OUTBOUND,
                -quantity, before, after, referenceId, null);
        stockLogRepository.save(log);
    }
}
