package com.stockandorder.domain.order.repository;

import com.stockandorder.domain.order.dto.PurchaseOrderListResponse;
import com.stockandorder.domain.order.dto.PurchaseOrderSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PurchaseOrderRepositoryCustom {

    Page<PurchaseOrderListResponse> search(PurchaseOrderSearchCondition condition, Pageable pageable);
}
