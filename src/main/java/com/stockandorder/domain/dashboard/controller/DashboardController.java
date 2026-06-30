package com.stockandorder.domain.dashboard.controller;

import com.stockandorder.domain.dashboard.service.DashboardService;
import com.stockandorder.global.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("memberName", userDetails.getName());
        model.addAttribute("role", userDetails.getRole());
        model.addAttribute("dashboard", dashboardService.getDashboard());
        return "dashboard";
    }
}
