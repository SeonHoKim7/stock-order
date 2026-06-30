package com.stockandorder.domain.inbound.repository;

import com.stockandorder.domain.inbound.entity.Inbound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface InboundRepository extends JpaRepository<Inbound, Long>, InboundRepositoryCustom {

    @Query("SELECT MAX(i.inboundNumber) FROM Inbound i WHERE i.inboundNumber LIKE :prefix%")
    Optional<String> findMaxInboundNumberByPrefix(@Param("prefix") String prefix);

    // 대시보드 "오늘 입고" 집계용. 기준은 created_at(시스템 등록 시각): inbound_date는 사람이 소급
    // 입력할 수 있어 활동 지표로는 흔들리고, 출고엔 그런 비즈니스 날짜가 없어 두 위젯의 "오늘"을 맞춘다.
    // 경계는 [start, end)로 받아 자정 포함 문제를 피한다(end는 다음 날 0시).
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime start, LocalDateTime end);

    // 상세 조회용: 발주/거래처/처리자/입고 항목/발주 항목/상품을 한 번에 로딩(N+1 방지).
    // 컬렉션 fetch join은 items 하나뿐이고 나머지는 ManyToOne이라 함께 fetch 가능.
    @Query("SELECT DISTINCT i FROM Inbound i " +
            "JOIN FETCH i.purchaseOrder o " +
            "JOIN FETCH o.supplier " +
            "JOIN FETCH i.processor " +
            "JOIN FETCH i.items it " +
            "JOIN FETCH it.orderItem oi " +
            "JOIN FETCH oi.product " +
            "WHERE i.inboundId = :inboundId")
    Optional<Inbound> findDetailById(@Param("inboundId") Long inboundId);
}
