package com.stockandorder.global.auth;

import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security의 UserDetails를 구현한 인증 주체.
 * Member 엔티티를 직접 들고 다니지 않고 필요한 정보만 꺼내 쓸 수 있도록 편의 메서드를 제공한다.
 * 세션에 저장되므로 Member 엔티티 전체를 참조하는 대신 id/role 등 최소 정보 접근 위주로 설계한다.
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final Member member;

    public CustomUserDetails(Member member) {
        this.member = member;
    }

    public Long getMemberId() {
        return member.getMemberId();
    }

    public String getName() {
        return member.getName();
    }

    public Role getRole() {
        return member.getRole();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()));
    }

    @Override
    public String getPassword() {
        return member.getPassword();
    }

    /** Spring Security에서 로그인 식별자로 사용하는 값 (loginId) */
    @Override
    public String getUsername() {
        return member.getLoginId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** isActive = false 이면 Spring Security가 DisabledException 발생 → 로그인 실패 처리 */
    @Override
    public boolean isEnabled() {
        return member.isActive();
    }
}
