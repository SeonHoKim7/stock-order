package com.stockandorder.domain.stock.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.IllegalTransactionStateException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * increase/decrease의 Propagation.MANDATORY 검증.
 *
 * 이 테스트만 @SpringBootTest인 이유: @Transactional은 스프링이 만든 프록시를 거쳐야 동작한다.
 * StockServiceTest는 Mockito 단위 테스트라 StockService가 프록시가 아닌 순수 객체여서,
 * 전파 속성을 무엇으로 바꿔도 통과해버린다. 즉 전파 속성은 단위 테스트로 검증할 수 없다.
 *
 * 여기서는 컨테이너에서 주입받은 빈(=프록시)을 트랜잭션 없이 호출해, 재고를 건드리기 전에
 * 호출 자체가 거부되는지 확인한다.
 */
@SpringBootTest
class StockServiceTransactionTest {

    @Autowired
    private StockService stockService;

    @Test
    @DisplayName("increase: 트랜잭션 없이 호출하면 IllegalTransactionStateException으로 즉시 거부된다")
    void increase_withoutTransaction_throws() {
        assertThatThrownBy(() -> stockService.increase(1L, 10, 1L, 1L))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    @DisplayName("decrease: 트랜잭션 없이 호출하면 IllegalTransactionStateException으로 즉시 거부된다")
    void decrease_withoutTransaction_throws() {
        assertThatThrownBy(() -> stockService.decrease(1L, 10, 1L, 1L))
                .isInstanceOf(IllegalTransactionStateException.class);
    }
}
