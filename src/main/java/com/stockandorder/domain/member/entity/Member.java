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


    // boolean 필드에 is 접두사가 붙으면 Hibernate가 컬럼명을 'active'로 매핑할 수 있어 명시적으로 지정
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    // 자체 회원가입 없이 관리자가 계정을 직접 생성하므로 역할을 파라미터로 받는다
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
