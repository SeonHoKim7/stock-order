package com.stockandorder.domain.stock.service;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.domain.stock.entity.Stock;
import com.stockandorder.domain.stock.entity.StockLog;
import com.stockandorder.domain.stock.enums.StockChangeType;
import com.stockandorder.domain.stock.repository.StockLogRepository;
import com.stockandorder.domain.stock.repository.StockRepository;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @InjectMocks
    private StockService stockService;

    @Mock
    private StockRepository stockRepository;
    @Mock
    private StockLogRepository stockLogRepository;

    private static final long PRODUCT_ID = 10L;
    private static final long REFERENCE_ID = 1000L;

    private Product product;
    private Stock stock;

    @BeforeEach
    void setUp() {
        Category category = Category.create("식자재", null);
        product = Product.create("PRD-001", "밀가루", category, "KG",
                BigDecimal.valueOf(10000), 10, null);
        ReflectionTestUtils.setField(product, "productId", PRODUCT_ID);
        stock = Stock.create(product);
    }

    @Test
    @DisplayName("increase: 비관적 락으로 재고를 조회해 수량을 늘린다")
    void increase_increasesStockQuantity() {
        given(stockRepository.findByProductIdForUpdate(PRODUCT_ID)).willReturn(Optional.of(stock));

        stockService.increase(PRODUCT_ID, 6, REFERENCE_ID);

        assertThat(stock.getQuantity()).isEqualTo(6);
    }

    @Test
    @DisplayName("increase: 변동 이력을 INBOUND 타입으로 before/after·referenceId와 함께 기록한다")
    void increase_recordsStockLog() {
        stock.increase(4); // 기존 재고 4
        given(stockRepository.findByProductIdForUpdate(PRODUCT_ID)).willReturn(Optional.of(stock));

        stockService.increase(PRODUCT_ID, 6, REFERENCE_ID);

        ArgumentCaptor<StockLog> captor = ArgumentCaptor.forClass(StockLog.class);
        then(stockLogRepository).should().save(captor.capture());
        StockLog log = captor.getValue();
        assertThat(log.getProduct()).isEqualTo(product);
        assertThat(log.getChangeType()).isEqualTo(StockChangeType.INBOUND);
        assertThat(log.getChangeQuantity()).isEqualTo(6);
        assertThat(log.getBeforeQuantity()).isEqualTo(4);
        assertThat(log.getAfterQuantity()).isEqualTo(10);
        assertThat(log.getReferenceId()).isEqualTo(REFERENCE_ID);
        assertThat(log.getReason()).isNull();
    }

    @Test
    @DisplayName("increase: 재고 레코드가 없으면 STOCK_NOT_FOUND 예외가 발생하고 로그를 남기지 않는다")
    void increase_stockNotFound_throws() {
        given(stockRepository.findByProductIdForUpdate(PRODUCT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.increase(PRODUCT_ID, 6, REFERENCE_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.STOCK_NOT_FOUND));
        then(stockLogRepository).should(never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("decrease: 비관적 락으로 재고를 조회해 수량을 줄인다")
    void decrease_decreasesStockQuantity() {
        stock.increase(10); // 기존 재고 10
        given(stockRepository.findByProductIdForUpdate(PRODUCT_ID)).willReturn(Optional.of(stock));

        stockService.decrease(PRODUCT_ID, 6, REFERENCE_ID);

        assertThat(stock.getQuantity()).isEqualTo(4);
    }

    @Test
    @DisplayName("decrease: 변동 이력을 OUTBOUND 타입으로 음수 변동량·before/after·referenceId와 함께 기록한다")
    void decrease_recordsStockLog() {
        stock.increase(10); // 기존 재고 10
        given(stockRepository.findByProductIdForUpdate(PRODUCT_ID)).willReturn(Optional.of(stock));

        stockService.decrease(PRODUCT_ID, 6, REFERENCE_ID);

        ArgumentCaptor<StockLog> captor = ArgumentCaptor.forClass(StockLog.class);
        then(stockLogRepository).should().save(captor.capture());
        StockLog log = captor.getValue();
        assertThat(log.getProduct()).isEqualTo(product);
        assertThat(log.getChangeType()).isEqualTo(StockChangeType.OUTBOUND);
        assertThat(log.getChangeQuantity()).isEqualTo(-6); // 출고는 음수로 기록
        assertThat(log.getBeforeQuantity()).isEqualTo(10);
        assertThat(log.getAfterQuantity()).isEqualTo(4);
        assertThat(log.getReferenceId()).isEqualTo(REFERENCE_ID);
        assertThat(log.getReason()).isNull();
    }

    @Test
    @DisplayName("decrease: 재고가 부족하면 STOCK_INSUFFICIENT 예외가 발생하고 로그를 남기지 않는다")
    void decrease_insufficientStock_throws() {
        stock.increase(5); // 재고 5뿐인데 6 출고 시도
        given(stockRepository.findByProductIdForUpdate(PRODUCT_ID)).willReturn(Optional.of(stock));

        assertThatThrownBy(() -> stockService.decrease(PRODUCT_ID, 6, REFERENCE_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.STOCK_INSUFFICIENT));
        then(stockLogRepository).should(never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("decrease: 재고 레코드가 없으면 STOCK_NOT_FOUND 예외가 발생한다")
    void decrease_stockNotFound_throws() {
        given(stockRepository.findByProductIdForUpdate(PRODUCT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.decrease(PRODUCT_ID, 6, REFERENCE_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.STOCK_NOT_FOUND));
        then(stockLogRepository).should(never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("adjust: 목표 수량으로 delta를 계산해 재고를 맞추고 ADJUST 로그를 referenceId 없이 사유와 함께 기록한다")
    void adjust_setsQuantityToTargetAndLogs() {
        stock.increase(50); // 현재고 50
        given(stockRepository.findByProductIdForUpdate(PRODUCT_ID)).willReturn(Optional.of(stock));

        // 본 값 50, 실사 결과 47 → delta -3
        stockService.adjust(PRODUCT_ID, 47, 50, "재고 실사 보정");

        assertThat(stock.getQuantity()).isEqualTo(47);

        ArgumentCaptor<StockLog> captor = ArgumentCaptor.forClass(StockLog.class);
        then(stockLogRepository).should().save(captor.capture());
        StockLog log = captor.getValue();
        assertThat(log.getChangeType()).isEqualTo(StockChangeType.ADJUST);
        assertThat(log.getChangeQuantity()).isEqualTo(-3);
        assertThat(log.getBeforeQuantity()).isEqualTo(50);
        assertThat(log.getAfterQuantity()).isEqualTo(47);
        assertThat(log.getReferenceId()).isNull();
        assertThat(log.getReason()).isEqualTo("재고 실사 보정");
    }

    @Test
    @DisplayName("adjust: 본 값(seenQuantity)이 락 시점의 실제 현재고와 다르면 STOCK_CONCURRENT_MODIFICATION으로 거부한다")
    void adjust_seenQuantityMismatch_throws() {
        stock.increase(45); // 실제 현재고는 45인데
        given(stockRepository.findByProductIdForUpdate(PRODUCT_ID)).willReturn(Optional.of(stock));

        // 사용자는 50을 보고 입력 → think-time 동안 변동된 것으로 간주
        assertThatThrownBy(() -> stockService.adjust(PRODUCT_ID, 47, 50, "사유"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.STOCK_CONCURRENT_MODIFICATION));
        assertThat(stock.getQuantity()).isEqualTo(45); // 변경 없음
        then(stockLogRepository).should(never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("adjust: 목표 수량이 현재고와 같으면(delta 0) STOCK_ADJUST_NO_CHANGE로 거부하고 로그를 남기지 않는다")
    void adjust_noChange_throws() {
        stock.increase(50);
        given(stockRepository.findByProductIdForUpdate(PRODUCT_ID)).willReturn(Optional.of(stock));

        assertThatThrownBy(() -> stockService.adjust(PRODUCT_ID, 50, 50, "사유"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.STOCK_ADJUST_NO_CHANGE));
        then(stockLogRepository).should(never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("adjust: 재고 레코드가 없으면 STOCK_NOT_FOUND 예외가 발생한다")
    void adjust_stockNotFound_throws() {
        given(stockRepository.findByProductIdForUpdate(PRODUCT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.adjust(PRODUCT_ID, 47, 50, "사유"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.STOCK_NOT_FOUND));
        then(stockLogRepository).should(never()).save(org.mockito.ArgumentMatchers.any());
    }
}
