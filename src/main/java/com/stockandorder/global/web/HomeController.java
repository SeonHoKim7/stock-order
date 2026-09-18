package com.stockandorder.global.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 루트("/") 진입 처리.
 * 로그인 사용자는 대시보드로, 미로그인 사용자는 /dashboard 접근 시 Security 가 /login 으로 보낸다.
 * 즉 인증 분기는 Security 에 맡기고, 루트는 "이 사람이 있어야 할 곳"인 대시보드로만 보낸다.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }
}
