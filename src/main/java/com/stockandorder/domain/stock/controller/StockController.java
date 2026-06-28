package com.stockandorder.domain.stock.controller;

import com.stockandorder.domain.category.service.CategoryService;
import com.stockandorder.domain.stock.dto.StockSearchCondition;
import com.stockandorder.domain.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/stocks")
public class StockController {

    private final StockService stockService;
    private final CategoryService categoryService;

    @GetMapping
    public String list(@ModelAttribute("condition") StockSearchCondition condition,
                       @PageableDefault(size = 10) Pageable pageable,
                       Model model) {
        model.addAttribute("stocks", stockService.searchStocks(condition, pageable));
        model.addAttribute("categories", categoryService.getCategories());
        return "stock/list";
    }
}
