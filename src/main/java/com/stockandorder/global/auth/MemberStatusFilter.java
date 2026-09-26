package com.stockandorder.global.auth;

import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * 인증된 요청마다 회원의 현재 상태를 DB에서 다시 확인하는 필터.
 *
 * [왜 필요한가]
 * {@link CustomUserDetails}는 로그인 시점의 스냅샷이라 세션이 살아있는 동안 값이 고정된다. 그래서
 * 관리자가 계정을 비활성화해도 이미 로그인한 사람은 세션이 만료될 때까지 계속 돌아다닐 수 있었고,
 * 권한을 STAFF에서 MANAGER로 올려도 다시 로그인하기 전까지는 반영되지 않았다.
 *
 * [한계]
 * 어느 방식이든 서버가 클라이언트를 먼저 끊을 수는 없다. 차단은 그 사용자의 다음 요청에서 일어난다.
 */
@RequiredArgsConstructor
public class MemberStatusFilter extends OncePerRequestFilter {

    private final MemberRepository memberRepository;

    // Spring Session이 HttpSession 구현을 갈아끼우므로, 이 저장소는 인메모리·Redis 양쪽에서 그대로 동작한다.
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<Member> found = memberRepository.findById(principal.getMemberId());

        // 비활성화됐거나 삭제된 계정이면 세션을 끊고 로그인 화면으로 돌려보낸다.
        if (found.isEmpty() || !found.get().isActive()) {
            invalidateSession(request);
            response.sendRedirect(request.getContextPath() + "/login?disabled=true");
            return;
        }

        // 권한이 바뀌었으면 세션의 인증 정보를 교체한다. 재로그인을 요구하지 않고 이번 요청부터 새 권한이 적용된다.
        Member member = found.get();
        if (member.getRole() != principal.getRole()) {
            refreshAuthentication(request, response, member, authentication);
        }

        filterChain.doFilter(request, response);
    }

    private void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    private void refreshAuthentication(HttpServletRequest request,
                                       HttpServletResponse response,
                                       Member member,
                                       Authentication previous) {
        CustomUserDetails refreshed = new CustomUserDetails(member);
        // 로그인 직후 Spring Security가 지우는 것과 같은 처리. 세션에 비밀번호 해시를 남기지 않는다.
        refreshed.eraseCredentials();

        UsernamePasswordAuthenticationToken renewed =
                new UsernamePasswordAuthenticationToken(refreshed, null, refreshed.getAuthorities());
        renewed.setDetails(previous.getDetails());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(renewed);
        SecurityContextHolder.setContext(context);
        // Spring Security 6부터 SecurityContextHolder 변경은 자동 저장되지 않으므로 명시적으로 기록한다.
        securityContextRepository.saveContext(context, request, response);
    }

    /** 로그인·로그아웃·정적 리소스는 검사 대상이 아니다(불필요한 조회를 줄인다). */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals("/login") || uri.equals("/logout") || uri.equals("/error")
                || uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/")
                || uri.equals("/favicon.ico");
    }
}
