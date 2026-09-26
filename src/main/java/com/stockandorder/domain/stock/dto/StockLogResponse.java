package com.stockandorder.domain.stock.dto;

import com.stockandorder.domain.stock.enums.StockChangeType;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 재고 변동 이력 한 줄. QueryDSL 프로젝션 대상(읽기 전용)이라 필요한 컬럼만 담는다.
 *
 * actorName은 null일 수 있다. 처리자 컬럼이 도입되기 전에 쌓인 로그와,
 * 그 이후 삭제된 회원이 남긴 로그가 여기에 해당한다.
 */
@Getter
public class StockLogResponse {

    private final LocalDateTime createdAt;
    private final Long productId;
    private final String productCode;
    private final String productName;
    private final StockChangeType changeType;
    private final int changeQuantity;
    private final int beforeQuantity;
    private final int afterQuantity;
    private final Long referenceId;
    private final String reason;
    private final String actorName;

    public StockLogResponse(LocalDateTime createdAt, Long productId, String productCode, String productName,
                            StockChangeType changeType, int changeQuantity, int beforeQuantity, int afterQuantity,
                            Long referenceId, String reason, String actorName) {
        this.createdAt = createdAt;
        this.productId = productId;
        this.productCode = productCode;
        this.productName = productName;
        this.changeType = changeType;
        this.changeQuantity = changeQuantity;
        this.beforeQuantity = beforeQuantity;
        this.afterQuantity = afterQuantity;
        this.referenceId = referenceId;
        this.reason = reason;
        this.actorName = actorName;
    }

    public String getChangeTypeLabel() {
        return changeType.getLabel();
    }

    /** 부호를 그대로 보여준다. 증가는 +10, 감소는 -3으로 읽히도록 양수에만 +를 붙인다. */
    public String getChangeQuantityLabel() {
        return changeQuantity > 0 ? "+" + changeQuantity : String.valueOf(changeQuantity);
    }

    public boolean isIncrease() {
        return changeQuantity > 0;
    }

    /**
     * 변동을 일으킨 원본 문서로 가는 링크 경로. ADJUST는 원본 문서가 없어 null이다.
     * referenceId는 FK 없는 다형 참조라, 어느 문서를 가리키는지는 changeType이 정한다.
     */
    public String getReferenceUrl() {
        if (referenceId == null) {
            return null;
        }
        return switch (changeType) {
            case INBOUND -> "/inbounds/" + referenceId;
            case OUTBOUND -> "/outbounds/" + referenceId;
            case ADJUST -> null;
        };
    }
}
