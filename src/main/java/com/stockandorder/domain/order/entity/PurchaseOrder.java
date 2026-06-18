package com.stockandorder.domain.order.entity;

import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.order.enums.OrderStatus;
import com.stockandorder.domain.supplier.entity.Supplier;
import com.stockandorder.global.common.BaseTimeEntity;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private Member requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private Member approver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    private LocalDateTime processedAt;

    // 입고 동시성 제어용 낙관적 락. 발주는 저경합이라 비관적 락 대신 @Version + 재시도로 처리한다.
    @Version
    private Long version;

    @Column(length = 500)
    private String rejectReason;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> items = new ArrayList<>();

    public static PurchaseOrder create(String orderNumber, Supplier supplier, Member requester, String note) {
        PurchaseOrder order = new PurchaseOrder();
        order.orderNumber = orderNumber;
        order.supplier = supplier;
        order.requester = requester;
        order.note = note;
        order.status = OrderStatus.PENDING;
        order.totalAmount = BigDecimal.ZERO;
        order.orderedAt = LocalDateTime.now();
        return order;
    }

    public void addItem(PurchaseOrderItem item) {
        validatePendingStatus(ErrorCode.ORDER_CANNOT_MODIFY);
        items.add(item);
        item.setPurchaseOrder(this);
        calculateTotalAmount();
    }

    public void removeItem(PurchaseOrderItem item) {
        validatePendingStatus(ErrorCode.ORDER_CANNOT_MODIFY);
        items.remove(item);
        calculateTotalAmount();
    }

    public void approve(Member approver) {
        validatePendingStatus(ErrorCode.ORDER_STATUS_CANNOT_APPROVE);
        this.status = OrderStatus.APPROVED;
        this.approver = approver;
        this.processedAt = LocalDateTime.now();
    }

    public void reject(Member approver, String rejectReason) {
        validatePendingStatus(ErrorCode.ORDER_STATUS_CANNOT_REJECT);
        this.status = OrderStatus.REJECTED;
        this.approver = approver;
        this.rejectReason = rejectReason;
        this.processedAt = LocalDateTime.now();
    }

    public void cancel() {
        validatePendingStatus(ErrorCode.ORDER_STATUS_CANNOT_CANCEL);
        this.status = OrderStatus.CANCELLED;
    }

    public void changeOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    /** 입고 가능한 발주인지 검증. 승인(APPROVED) 또는 부분 입고 진행 중(IN_PROGRESS)만 허용. */
    public void validateReceivable() {
        if (this.status != OrderStatus.APPROVED && this.status != OrderStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.INBOUND_ORDER_NOT_APPROVED);
        }
    }

    /**
     * 입고 누적 결과로 발주 상태를 재계산한다. 상태는 항목별이 아니라 발주서 전체 하나.
     * 모든 항목이 발주 수량만큼 입고되면 COMPLETED, 하나라도 덜 차면 IN_PROGRESS.
     */
    public void refreshStatusByReceipt() {
        boolean allReceived = items.stream()
                .allMatch(item -> item.getReceivedQuantity() == item.getQuantity());
        this.status = allReceived ? OrderStatus.COMPLETED : OrderStatus.IN_PROGRESS;
    }

    private void validatePendingStatus(ErrorCode errorCode) {
        if (this.status != OrderStatus.PENDING) {
            throw new BusinessException(errorCode);
        }
    }

    private void calculateTotalAmount() {
        this.totalAmount = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
