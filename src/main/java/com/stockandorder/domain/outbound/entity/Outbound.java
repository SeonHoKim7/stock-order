package com.stockandorder.domain.outbound.entity;

import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.supplier.entity.Supplier;
import com.stockandorder.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 출고(출고 영수증). 판매처로 물건을 내보낸 "사실"을 기록하는 사건(event)이다.
 *
 * 설계 결정:
 * - 1: status 없음. 생성 즉시 확정이며 재고도 그 순간 차감된다(입고와 동일한 단일화). 요청/확정 2단계,
 *   confirm()/cancel(), 취소(CANCELLED)는 모두 두지 않는다. 출고 취소는 Phase 7 재고조정으로 처리한다.
 * - A-1: OutboundItem과 한 Aggregate. cascade ALL + orphanRemoval. 생성자에서만 항목이 채워지는
 *   완전 불변 객체(addItem/removeItem 없음).
 * - A-2: 상위 문서가 없는 독립 사건. 발주가 아니라 판매처(Supplier)를 직접 N:1로 참조한다.
 * - 3: totalAmount(매출 총액)는 항목들의 매출가 스냅샷 합으로 생성 시 자동 계산한다(서비스 누락 방지).
 * - F-2: outbound_date(실제 출고일, 사람 입력·소급 가능)는 created_at(시스템 자동·불변)과 별개의 사실.
 * - F-4: processor(출고 처리자)는 추적·책임 소재를 위해 NOT NULL.
 */
@Entity
@Table(name = "outbound")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Outbound extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long outboundId;

    @Column(nullable = false, unique = true, length = 50)
    private String outboundNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_id", nullable = false)
    private Member processor;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private LocalDate outboundDate;

    @Column(length = 500)
    private String note;

    @OneToMany(mappedBy = "outbound", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OutboundItem> items = new ArrayList<>();

    /**
     * 출고는 생성 시 항목까지 통째로 확정되는 불변 객체다. 항목을 채우는 통로는 이 생성자 하나뿐이며,
     * 매출 총액도 여기서 항목 합산으로 확정한다.
     */
    public static Outbound create(String outboundNumber,
                                  Supplier supplier,
                                  Member processor,
                                  LocalDate outboundDate,
                                  String note,
                                  List<OutboundItem> items) {
        Outbound outbound = new Outbound();
        outbound.outboundNumber = outboundNumber;
        outbound.supplier = supplier;
        outbound.processor = processor;
        outbound.outboundDate = outboundDate;
        outbound.note = note;
        for (OutboundItem item : items) {
            outbound.items.add(item);
            item.setOutbound(outbound);
        }
        outbound.totalAmount = calculateTotalAmount(items);
        return outbound;
    }

    private static BigDecimal calculateTotalAmount(List<OutboundItem> items) {
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
