package com.stockandorder.global.config;

import com.stockandorder.domain.dashboard.controller.DashboardController;
import com.stockandorder.domain.member.controller.AuthController;
import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import com.stockandorder.global.auth.CustomUserDetails;
import com.stockandorder.global.auth.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, DashboardController.class})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("미인증 상태로 /dashboard 접근 시 로그인 페이지로 리다이렉트된다")
    void unauthenticated_dashboard_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("미인증 상태로 /login 접근 시 200 OK")
    void unauthenticated_loginPage_returns200() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증된 사용자는 /dashboard에 접근할 수 있다")
    void authenticated_dashboard_returns200() throws Exception {
        CustomUserDetails userDetails = staffUserDetails();

        mockMvc.perform(get("/dashboard").with(user(userDetails)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("STAFF 권한으로 /admin/** 접근 시 403")
    void staff_adminUrl_returns403() throws Exception {
        CustomUserDetails userDetails = staffUserDetails();

        mockMvc.perform(get("/admin/members").with(user(userDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("올바른 아이디/비밀번호로 로그인 시 /dashboard로 리다이렉트된다")
    void login_validCredentials_redirectsToDashboard() throws Exception {
        String encodedPassword = passwordEncoder.encode("admin1234");
        given(customUserDetailsService.loadUserByUsername("admin"))
                .willReturn(adminUserDetails(encodedPassword));

        mockMvc.perform(post("/login")
                        .param("loginId", "admin")
                        .param("password", "admin1234")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("틀린 비밀번호로 로그인 시 /login?error=true로 리다이렉트된다")
    void login_wrongPassword_redirectsToLoginWithError() throws Exception {
        String encodedPassword = passwordEncoder.encode("admin1234");
        given(customUserDetailsService.loadUserByUsername("admin"))
                .willReturn(adminUserDetails(encodedPassword));

        mockMvc.perform(post("/login")
                        .param("loginId", "admin")
                        .param("password", "wrongPassword")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("비활성화된 계정으로 로그인 시 /login?error=true로 리다이렉트된다")
    void login_deactivatedAccount_redirectsToLoginWithError() throws Exception {
        String encodedPassword = passwordEncoder.encode("admin1234");
        Member member = Member.create("admin", encodedPassword, "관리자", null, Role.ADMIN);
        member.deactivate();
        given(customUserDetailsService.loadUserByUsername("admin"))
                .willReturn(new CustomUserDetails(member));

        mockMvc.perform(post("/login")
                        .param("loginId", "admin")
                        .param("password", "admin1234")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("로그아웃 시 /login?logout=true로 리다이렉트된다")
    void logout_redirectsToLoginWithLogout() throws Exception {
        mockMvc.perform(post("/logout")
                        .with(user(staffUserDetails()))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }

    private CustomUserDetails staffUserDetails() {
        Member member = Member.create("staff01", "encoded", "홍길동", null, Role.STAFF);
        return new CustomUserDetails(member);
    }

    private CustomUserDetails adminUserDetails(String encodedPassword) {
        Member member = Member.create("admin", encodedPassword, "관리자", null, Role.ADMIN);
        return new CustomUserDetails(member);
    }
}
