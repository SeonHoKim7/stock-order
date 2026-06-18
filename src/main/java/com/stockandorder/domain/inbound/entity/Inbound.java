package com.stockandorder.domain.inbound.entity;

import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.order.entity.PurchaseOrder;
import com.stockandorder.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 입고(입고 영수증). 물건이 실제로 도착한 "사실"을 기록하는 사건(event)이다.
 *
 * 설계 결정:
 * - A-1: InboundItem과 한 Aggregate. cascade ALL + orphanRemoval. 단, 발주와 달리 생성 후 항목이 고정되는
 *   불변 객체로 설계한다(addItem/removeItem 없음, 항목은 생성자에서만 채워짐).
 * - A-2: PurchaseOrder와는 다른 Aggregate. 단방향 N:1(발주 1 : 입고 N, 부분 입고)로만 참조한다.
 * - F-1: 입고번호 IN-yyyyMMdd-NNN (서비스에서 생성).
 * - F-2: inbound_date(실제 입고일, 사람이 입력·소급 가능)는 created_at(시스템 자동·불변)과 별개의 사실.
 * - F-3: status 컬럼 없음. 생성 즉시 확정이며 취소 불가(낙장불입). "대기/예정"은 발주가 표현한다.
 * - F-4: processor(입고 처리자)는 추적·책임 소재를 위해 NOT NULL.
 */
@Entity
@Table(name = "inbound")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inbound extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inboundId;

    @Column(nullable = false, unique = true, length = 50)
    private String inboundNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_id", nullable = false)
    private Member processor;

    @Column(nullable = false)
    private LocalDate inboundDate;

    @Column(length = 500)
    private String note;

    @OneToMany(mappedBy = "inbound", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InboundItem> items = new ArrayList<>();

    /**
     * 입고는 생성 시 항목까지 통째로 확정되는 불변 객체다. 항목을 채우는 통로는 이 생성자 하나뿐이며,
     * 이후 항목 추가/삭제 메서드를 제공하지 않아 "입고는 한 번 기록되면 변하지 않는다"를 구조로 강제한다.
     */
    public static Inbound create(String inboundNumber,
                                 PurchaseOrder purchaseOrder,
                                 Member processor,
                                 LocalDate inboundDate,
                                 String note,
                                 List<InboundItem> items) {
        Inbound inbound = new Inbound();
        inbound.inboundNumber = inboundNumber;
        inbound.purchaseOrder = purchaseOrder;
        inbound.processor = processor;
        inbound.inboundDate = inboundDate;
        inbound.note = note;
        for (InboundItem item : items) {
            inbound.items.add(item);
            item.setInbound(inbound);
        }
        return inbound;
    }
}
