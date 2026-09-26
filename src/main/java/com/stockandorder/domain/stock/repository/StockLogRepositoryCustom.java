package com.stockandorder.domain.stock.repository;

import com.stockandorder.domain.stock.dto.StockLogResponse;
import com.stockandorder.domain.stock.dto.StockLogSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockLogRepositoryCustom {

    Page<StockLogResponse> search(StockLogSearchCondition condition, Pageable pageable);
}
