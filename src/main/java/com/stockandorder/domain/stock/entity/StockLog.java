package com.stockandorder.domain.stock.entity;

import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.domain.stock.enums.StockChangeType;
import com.stockandorder.global.common.BaseTimeEntity;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "stock_log",
        indexes = @Index(
                name = "idx_stock_log_product_created",
                columnList = "product_id, created_at"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stockLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockChangeType changeType;

    // 부호 포함(signed): INBOUND>0, OUTBOUND<0, ADJUST≠0
    @Column(nullable = false)
    private int changeQuantity;

    @Column(nullable = false)
    private int beforeQuantity;

    @Column(nullable = false)
    private int afterQuantity;

    // INBOUND→inbound_id, OUTBOUND→outbound_id, ADJUST→null. FK 없이 단순 Long.
    private Long referenceId;

    @Column(length = 500)
    private String reason;

    public static StockLog of(Product product,
                              StockChangeType changeType,
                              int changeQuantity,
                              int beforeQuantity,
                              int afterQuantity,
                              Long referenceId,
                              String reason) {
        validateDirection(changeType, changeQuantity);
        validateAdjustReason(changeType, reason);
        if (afterQuantity != beforeQuantity + changeQuantity) {
            throw new IllegalArgumentException(
                    "after_quantity 는 before_quantity + change_quantity 와 일치해야 합니다.");
        }
        StockLog log = new StockLog();
        log.product = product;
        log.changeType = changeType;
        log.changeQuantity = changeQuantity;
        log.beforeQuantity = beforeQuantity;
        log.afterQuantity = afterQuantity;
        log.referenceId = referenceId;
        log.reason = reason;
        return log;
    }

    private static void validateDirection(StockChangeType type, int changeQuantity) {
        switch (type) {
            case INBOUND -> {
                if (changeQuantity <= 0) {
                    throw new IllegalArgumentException("INBOUND 변동량은 양수여야 합니다.");
                }
            }
            case OUTBOUND -> {
                if (changeQuantity >= 0) {
                    throw new IllegalArgumentException("OUTBOUND 변동량은 음수여야 합니다.");
                }
            }
            case ADJUST -> {
                if (changeQuantity == 0) {
                    throw new IllegalArgumentException("ADJUST 변동량은 0이 될 수 없습니다.");
                }
            }
        }
    }

    // ADJUST는 원본 문서(referenceId)가 없어 reason이 변동의 유일한 추적 단서다(I-4).
    // 사유 없는 조정 로그는 존재 자체가 불가능하도록 생성 지점에서 막는다(불변식).
    // DTO @NotBlank가 사용자 입력을 먼저 거르지만, 이 검증은 호출 경로와 무관하게 규칙을 보장한다.
    private static void validateAdjustReason(StockChangeType type, String reason) {
        if (type == StockChangeType.ADJUST && (reason == null || reason.isBlank())) {
            throw new BusinessException(ErrorCode.STOCK_ADJUST_REASON_REQUIRED);
        }
    }
}
