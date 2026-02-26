package com.stockandorder.domain.member.entity;

import com.stockandorder.domain.member.enums.Role;
import com.stockandorder.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    @Column(nullable = false, unique = true, length = 50)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;


    // is 접두사 필드는 Hibernate가 컬럼명을 'active'로 잘못 매핑할 수 있어 명시적으로 지정
    // 로그인 시 Spring Security가 자동으로 로그인 체크를 하기 위함. CustomUserDetailsService를 통해
    // 따라서, 비활성화 된 계정이라면 올바르게 로그인 해도 막힘. why? 관리자가 계정 활성화/비활성화 하기 위함
    // + 향후 회원 목록 조회 시, 활성화 된 회원만 조회하도록
    @Column(name = "is_active", nullable = false)
    private boolean isActive;


    // 팩토리 메서드 - 생성 시점의 불변식 보장
    // 자체 회원가입은 없고 관리자가 계정을 생성하므로 역할을 명시적으로 받는다.

    public static Member create(String loginId, String encodedPassword, String name, String email, Role role) {
        Member member = new Member();
        member.loginId = loginId;
        member.password = encodedPassword;
        member.name = name;
        member.email = email;
        member.role = role;
        member.isActive = true;
        return member;
    }


    // 비즈니스 메서드 - setter 대신 의도가 드러나는 메서드로 상태 변경

    public void updateProfile(String name, String email, Role role) {
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public void changeRole(Role role) {
        this.role = role;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }
}
