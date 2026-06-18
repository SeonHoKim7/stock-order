package com.stockandorder.domain.inbound.entity;

import com.stockandorder.domain.order.entity.PurchaseOrderItem;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 입고 항목(입고 영수증의 한 줄). "어느 발주 항목에 대해 몇 개 받았는가"를 기록한다.
 *
 * 설계 결정:
 * - A-3: PurchaseOrderItem과 단방향 N:1(FK order_item_id). 부분 입고의 연결고리이며, 이 끈으로
 *   누적 입고 수량 갱신과 잔여 수량 검증이 가능하다.
 * - B-2: product를 중복 보관하지 않는다. 상품은 orderItem.product로 도달(입고 처리 시 receivedQuantity
 *   갱신 때문에 orderItem을 어차피 로딩하므로 fetch join 으로 공짜로 딸려옴).
 * - B-3: 금액(unitPrice/totalAmount)을 두지 않는다. 입고는 "수량 흐름"만 책임지고 금전은 발주가 책임진다.
 */
@Entity
@Table(name = "inbound_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InboundItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inboundItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inbound_id", nullable = false)
    private Inbound inbound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private PurchaseOrderItem orderItem;

    @Column(nullable = false)
    private int quantity;

    public static InboundItem create(PurchaseOrderItem orderItem, int quantity) {
        InboundItem item = new InboundItem();
        item.orderItem = orderItem;
        item.quantity = quantity;
        return item;
    }

    void setInbound(Inbound inbound) {
        this.inbound = inbound;
    }
}
