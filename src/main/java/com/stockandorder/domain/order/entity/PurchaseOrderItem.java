package com.stockandorder.domain.order.entity;

import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int receivedQuantity;

    public static PurchaseOrderItem create(Product product, int quantity, BigDecimal unitPrice) {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.product = product;
        item.quantity = quantity;
        item.unitPrice = unitPrice;
        item.receivedQuantity = 0;
        return item;
    }

    void setPurchaseOrder(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    /**
     * 입고 수량 누적. 초과 입고 금지 불변식(누적 입고량 ≤ 발주 수량)을 엔티티가 스스로 보호한다.
     * receivedQuantity를 변경하는 유일한 통로이므로 어디서 호출하든 검증을 우회할 수 없다.
     */
    public void receive(int quantity) {
        if (this.receivedQuantity + quantity > this.quantity) {
            throw new BusinessException(ErrorCode.INBOUND_QUANTITY_EXCEEDED);
        }
        this.receivedQuantity += quantity;
    }
}
