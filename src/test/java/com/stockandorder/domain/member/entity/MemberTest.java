package com.stockandorder.domain.member.entity;

import com.stockandorder.domain.member.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.create("staff01", "encodedPassword", "홍길동", "hong@test.com", Role.STAFF);
    }

    @Test
    @DisplayName("계정 생성 시 isActive는 true다")
    void create_isActiveTrue() {
        assertThat(member.isActive()).isTrue();
    }

    @Test
    @DisplayName("계정 생성 시 전달한 값들이 올바르게 설정된다")
    void create_fieldsAreSet() {
        assertThat(member.getLoginId()).isEqualTo("staff01");
        assertThat(member.getPassword()).isEqualTo("encodedPassword");
        assertThat(member.getName()).isEqualTo("홍길동");
        assertThat(member.getEmail()).isEqualTo("hong@test.com");
        assertThat(member.getRole()).isEqualTo(Role.STAFF);
    }

    @Test
    @DisplayName("deactivate() 호출 시 isActive가 false가 된다")
    void deactivate_setsIsActiveFalse() {
        member.deactivate();

        assertThat(member.isActive()).isFalse();
    }

    @Test
    @DisplayName("비활성화된 계정을 activate() 하면 isActive가 true가 된다")
    void activate_setsIsActiveTrue() {
        member.deactivate();
        member.activate();

        assertThat(member.isActive()).isTrue();
    }

    @Test
    @DisplayName("changeRole() 호출 시 역할이 변경된다")
    void changeRole_updatesRole() {
        member.changeRole(Role.MANAGER);

        assertThat(member.getRole()).isEqualTo(Role.MANAGER);
    }

    @Test
    @DisplayName("changePassword() 호출 시 비밀번호가 변경된다")
    void changePassword_updatesPassword() {
        member.changePassword("newEncodedPassword");

        assertThat(member.getPassword()).isEqualTo("newEncodedPassword");
    }
}
