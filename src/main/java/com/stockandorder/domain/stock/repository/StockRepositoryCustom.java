package com.stockandorder.domain.stock.repository;

import com.stockandorder.domain.stock.dto.StockListResponse;
import com.stockandorder.domain.stock.dto.StockSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockRepositoryCustom {

    Page<StockListResponse> search(StockSearchCondition condition, Pageable pageable);
}
