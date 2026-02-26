package com.stockandorder.global.auth;

import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserDetailsTest {

    @Test
    @DisplayName("getUsername()은 loginId를 반환한다")
    void getUsername_returnsLoginId() {
        Member member = Member.create("manager01", "encoded", "김매니저", null, Role.MANAGER);
        CustomUserDetails userDetails = new CustomUserDetails(member);

        assertThat(userDetails.getUsername()).isEqualTo("manager01");
    }

    @Test
    @DisplayName("getPassword()는 Member의 password를 반환한다")
    void getPassword_returnsMemberPassword() {
        Member member = Member.create("staff01", "encodedPassword", "홍길동", null, Role.STAFF);
        CustomUserDetails userDetails = new CustomUserDetails(member);

        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
    }

    @Test
    @DisplayName("getAuthorities()는 'ROLE_' 접두사가 붙은 권한을 반환한다")
    void getAuthorities_hasRolePrefix() {
        Member member = Member.create("admin01", "encoded", "관리자", null, Role.ADMIN);
        CustomUserDetails userDetails = new CustomUserDetails(member);

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("각 Role에 대해 올바른 권한 문자열이 반환된다")
    void getAuthorities_correctRoleString() {
        assertAuthority(Role.ADMIN, "ROLE_ADMIN");
        assertAuthority(Role.MANAGER, "ROLE_MANAGER");
        assertAuthority(Role.STAFF, "ROLE_STAFF");
    }

    @Test
    @DisplayName("활성화된 계정은 isEnabled()가 true다")
    void isEnabled_activeAccount_returnsTrue() {
        Member member = Member.create("staff01", "encoded", "홍길동", null, Role.STAFF);
        CustomUserDetails userDetails = new CustomUserDetails(member);

        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("비활성화된 계정은 isEnabled()가 false다")
    void isEnabled_deactivatedAccount_returnsFalse() {
        Member member = Member.create("staff01", "encoded", "홍길동", null, Role.STAFF);
        member.deactivate();
        CustomUserDetails userDetails = new CustomUserDetails(member);

        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("편의 메서드들이 Member의 값을 올바르게 반환한다")
    void convenienceMethods_returnMemberValues() {
        Member member = Member.create("staff01", "encoded", "홍길동", "hong@test.com", Role.STAFF);
        CustomUserDetails userDetails = new CustomUserDetails(member);

        assertThat(userDetails.getName()).isEqualTo("홍길동");
        assertThat(userDetails.getRole()).isEqualTo(Role.STAFF);
    }

    // -----------------------

    private void assertAuthority(Role role, String expectedAuthority) {
        Member member = Member.create("id", "pw", "name", null, role);
        CustomUserDetails userDetails = new CustomUserDetails(member);

        String actual = userDetails.getAuthorities().iterator().next().getAuthority();
        assertThat(actual).isEqualTo(expectedAuthority);
    }
}
