package com.stockandorder.domain.dashboard.controller;

import com.stockandorder.global.auth.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("memberName", userDetails.getName());
        model.addAttribute("role", userDetails.getRole());
        return "dashboard";
    }
}
