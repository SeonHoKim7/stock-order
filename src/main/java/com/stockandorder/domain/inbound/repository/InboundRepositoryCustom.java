package com.stockandorder.domain.inbound.repository;

import com.stockandorder.domain.inbound.dto.InboundListResponse;
import com.stockandorder.domain.inbound.dto.InboundSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InboundRepositoryCustom {

    Page<InboundListResponse> search(InboundSearchCondition condition, Pageable pageable);
}
