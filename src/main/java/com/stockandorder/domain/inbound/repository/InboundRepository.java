package com.stockandorder.domain.inbound.repository;

import com.stockandorder.domain.inbound.entity.Inbound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InboundRepository extends JpaRepository<Inbound, Long>, InboundRepositoryCustom {

    @Query("SELECT MAX(i.inboundNumber) FROM Inbound i WHERE i.inboundNumber LIKE :prefix%")
    Optional<String> findMaxInboundNumberByPrefix(@Param("prefix") String prefix);

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
