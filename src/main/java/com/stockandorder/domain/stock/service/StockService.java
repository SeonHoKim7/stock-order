package com.stockandorder.domain.stock.service;

import com.stockandorder.domain.stock.entity.Stock;
import com.stockandorder.domain.stock.entity.StockLog;
import com.stockandorder.domain.stock.enums.StockChangeType;
import com.stockandorder.domain.stock.repository.StockLogRepository;
import com.stockandorder.domain.stock.repository.StockRepository;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
