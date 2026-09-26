package com.stockandorder.global.auth;

import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import com.stockandorder.domain.member.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class MemberStatusFilterTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private FilterChain filterChain;

    private MemberStatusFilter filter;

    private static final long MEMBER_ID = 7L;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Member member;

    @BeforeEach
    void setUp() {
        filter = new MemberStatusFilter(memberRepository);

        request = new MockHttpServletRequest();
        request.setRequestURI("/stocks");
        request.setSession(new MockHttpSession());
        response = new MockHttpServletResponse();

        member = Member.create("staff1", "password", "직원1", "staff1@test.com", Role.STAFF);
        ReflectionTestUtils.setField(member, "memberId", MEMBER_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** 로그인 상태를 재현한다. 세션에 담긴 것은 로그인 시점의 스냅샷이다. */
    private void givenLoggedIn(Member snapshotSource) {
        CustomUserDetails principal = new CustomUserDetails(snapshotSource);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
    }

    @Test
    @DisplayName("비활성화된 계정의 요청은 세션을 끊고 로그인 화면으로 돌려보낸다")
    void deactivatedMember_isLoggedOut() throws Exception {
        givenLoggedIn(member); // 로그인 시점에는 활성 상태였다
        member.deactivate();   // 그 뒤 관리자가 비활성화
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?disabled=true");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // 요청이 보호 자원에 닿지 못한다
        then(filterChain).should(never()).doFilter(request, response);
    }

    @Test
    @DisplayName("삭제된 계정의 요청도 동일하게 차단된다")
    void unknownMember_isLoggedOut() throws Exception {
        givenLoggedIn(member);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?disabled=true");
        then(filterChain).should(never()).doFilter(request, response);
    }

    @Test
    @DisplayName("권한이 바뀌면 재로그인 없이 이번 요청부터 새 권한이 적용된다")
    void roleChanged_authenticationIsRefreshed() throws Exception {
        givenLoggedIn(member); // STAFF로 로그인
        member.changeRole(Role.MANAGER);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        filter.doFilter(request, response, filterChain);

        CustomUserDetails current = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        assertThat(current.getRole()).isEqualTo(Role.MANAGER);
        assertThat(current.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_MANAGER");
        // 세션에 비밀번호 해시를 남기지 않는다
        assertThat(current.getPassword()).isNull();
        then(filterChain).should().doFilter(request, response);
    }

    @Test
    @DisplayName("상태가 그대로면 인증 정보를 건드리지 않고 그대로 통과시킨다")
    void unchangedMember_passesThrough() throws Exception {
        givenLoggedIn(member);
        Object before = SecurityContextHolder.getContext().getAuthentication();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(before);
        then(filterChain).should().doFilter(request, response);
    }

    @Test
    @DisplayName("비로그인 요청은 회원 조회 없이 통과시킨다")
    void anonymousRequest_skipsLookup() throws Exception {
        filter.doFilter(request, response, filterChain);

        then(memberRepository).should(never()).findById(org.mockito.ArgumentMatchers.anyLong());
        then(filterChain).should().doFilter(request, response);
    }

    @Test
    @DisplayName("로그인·정적 리소스 경로는 검사 대상이 아니다")
    void staticAndLoginPaths_areNotFiltered() throws Exception {
        givenLoggedIn(member);
        request.setRequestURI("/css/app.css");

        filter.doFilter(request, response, filterChain);

        then(memberRepository).should(never()).findById(org.mockito.ArgumentMatchers.anyLong());
        then(filterChain).should().doFilter(request, response);
    }
}
