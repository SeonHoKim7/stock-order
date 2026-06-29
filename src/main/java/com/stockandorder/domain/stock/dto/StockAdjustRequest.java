package com.stockandorder.domain.stock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 재고 수동 조정 요청. 입력은 "증감"이 아니라 "목표 수량(실사로 직접 센 절대값)"으로 받는다.
 * delta = targetQuantity - 현재고 계산은 서버가 락 안에서 수행한다.
 *
 * seenQuantity: 사용자가 폼을 띄운 시점에 화면에서 본 현재고. think-time(폼 조회~제출 사이)에
 * 재고가 바뀌면 사용자의 실사값이 낡은 기준일 수 있으므로, 락 시점의 실제 현재고와 비교해
 * 다르면 거부한다(낙관적 검증). 비관적 락이 닿지 못하는 요청 간 구간을 막는 한 겹이다.
 */
@Getter
@Setter
public class StockAdjustRequest {

    @NotNull
    @Min(0)
    private Integer targetQuantity;

    @NotNull
    @Min(0)
    private Integer seenQuantity;

    // 조정은 referenceId가 없어 사유가 유일한 추적 단서이므로 필수.
    @NotBlank(message = "조정 사유를 입력해 주세요.")
    private String reason;
}
