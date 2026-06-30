package com.stockandorder.domain.outbound.repository;

import com.stockandorder.domain.outbound.entity.Outbound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OutboundRepository extends JpaRepository<Outbound, Long>, OutboundRepositoryCustom {

    @Query("SELECT MAX(o.outboundNumber) FROM Outbound o WHERE o.outboundNumber LIKE :prefix%")
    Optional<String> findMaxOutboundNumberByPrefix(@Param("prefix") String prefix);

    // 대시보드 "오늘 출고" 집계용. 입고와 동일하게 created_at 기준으로 [start, end)를 센다(입고 쪽 주석 참조).
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime start, LocalDateTime end);

    // 상세 조회용: 거래처/처리자/출고 항목/상품을 한 번에 로딩(N+1 방지).
    @Query("SELECT DISTINCT o FROM Outbound o " +
            "JOIN FETCH o.supplier " +
            "JOIN FETCH o.processor " +
            "JOIN FETCH o.items it " +
            "JOIN FETCH it.product " +
            "WHERE o.outboundId = :outboundId")
    Optional<Outbound> findDetailById(@Param("outboundId") Long outboundId);
}
