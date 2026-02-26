package com.stockandorder.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정.
 *
 * [인증 방식 선택 근거]
 * - JWT: Stateless하여 수평 확장에 유리하지만, 토큰 즉시 무효화(강제 로그아웃, 계정 비활성화 즉시 반영)가
 *   어렵다. 이 시스템은 사내 B2B이므로 관리자가 계정을 비활성화하면 즉시 세션을 끊을 수 있어야 한다.
 * - Session: 서버 측 세션이므로 즉시 무효화 가능. 현재 단일 서버 환경이므로 세션 불일치 문제 없음.
 *   → HttpSession 기반 세션 인증 채택. 향후 스케일아웃 필요 시 Redis Session으로 전환 예정임.
 *
 * [URL 접근 제어 전략]
 * - /admin/** : ADMIN 전용 (회원 관리)
 * - MANAGER+ 권한이 필요한 비즈니스 로직은 @PreAuthorize로 메서드 레벨에서 제어
 *   → URL 패턴이 복잡해지는 것을 방지하고, 권한 로직을 해당 컨트롤러/서비스에 명시적으로 표현
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * UserDetailsService와 PasswordEncoder를 명시적으로 연결한다.
     * 암묵적인 자동 연결에 의존하면 @WebMvcTest 슬라이스 환경에서
     * Mock이 인증 과정에 실제로 사용되지 않는 문제가 발생한다.
     * 따라서 테스트를 하기 위한 설계이다.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   DaoAuthenticationProvider authenticationProvider) throws Exception {
        http
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/error").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .usernameParameter("loginId")
                        .passwordParameter("password")
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }
}
