package com.stockandorder.domain.outbound.entity;

import com.stockandorder.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 출고 항목(출고 영수증의 한 줄). "어느 상품을 몇 개, 얼마에 내보냈는가"를 기록한다.
 *
 * 설계 결정:
 * - A-3: 입고와 달리 상위 문서(발주)가 없으므로 Product를 단방향 N:1로 직접 참조한다.
 * - 3: unitPrice는 출고 시점 Product.salePrice(매출가)를 스냅샷한다. 이후 상품 단가가 바뀌어도
 *   과거 출고 기록의 금액은 보존된다(발주 항목의 매입가 스냅샷과 같은 패턴).
 */
@Entity
@Table(name = "outbound_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboundItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long outboundItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outbound_id", nullable = false)
    private Outbound outbound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    public static OutboundItem create(Product product, int quantity, BigDecimal unitPrice) {
        OutboundItem item = new OutboundItem();
        item.product = product;
        item.quantity = quantity;
        item.unitPrice = unitPrice;
        return item;
    }

    void setOutbound(Outbound outbound) {
        this.outbound = outbound;
    }
}
