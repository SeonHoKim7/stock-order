package com.stockandorder.domain.stock.controller;

import com.stockandorder.domain.category.service.CategoryService;
import com.stockandorder.domain.stock.dto.StockAdjustRequest;
import com.stockandorder.domain.stock.dto.StockSearchCondition;
import com.stockandorder.domain.stock.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @GetMapping("/{productId}/adjust")
    public String adjustForm(@PathVariable Long productId, Model model) {
        model.addAttribute("stock", stockService.getAdjustForm(productId));
        model.addAttribute("form", new StockAdjustRequest());
        return "stock/adjust";
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @PostMapping("/{productId}/adjust")
    public String adjust(@PathVariable Long productId,
                         @Valid @ModelAttribute("form") StockAdjustRequest form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("stock", stockService.getAdjustForm(productId));
            return "stock/adjust";
        }
        stockService.adjust(productId, form.getTargetQuantity(), form.getSeenQuantity(), form.getReason());
        redirectAttributes.addFlashAttribute("message", "재고가 조정되었습니다.");
        return "redirect:/stocks";
    }
}
