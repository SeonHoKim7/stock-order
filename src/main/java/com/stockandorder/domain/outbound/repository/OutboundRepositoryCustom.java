package com.stockandorder.domain.outbound.repository;

import com.stockandorder.domain.outbound.dto.OutboundListResponse;
import com.stockandorder.domain.outbound.dto.OutboundSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OutboundRepositoryCustom {

    Page<OutboundListResponse> search(OutboundSearchCondition condition, Pageable pageable);
}
