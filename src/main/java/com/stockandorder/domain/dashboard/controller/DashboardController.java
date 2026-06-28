package com.stockandorder.domain.dashboard.controller;

import com.stockandorder.domain.stock.service.StockService;
import com.stockandorder.global.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    // 대시보드 경고 위젯에 보여줄 상위 건수
    private static final int LOW_STOCK_PREVIEW_LIMIT = 5;

    private final StockService stockService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("memberName", userDetails.getName());
        model.addAttribute("role", userDetails.getRole());
        // 건수(getTotalElements) + 상위 N개(getContent)가 한 Page에 함께 담긴다.
        model.addAttribute("lowStock", stockService.getLowStockPreview(LOW_STOCK_PREVIEW_LIMIT));
        return "dashboard";
    }
}
