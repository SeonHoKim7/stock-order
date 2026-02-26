package com.stockandorder.domain.member.dto;

import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MemberResponse {

    private final Long memberId;
    private final String loginId;
    private final String name;
    private final String email;
    private final Role role;
    private final boolean active;
    private final LocalDateTime createdAt;

    private MemberResponse(Member member) {
        this.memberId = member.getMemberId();
        this.loginId = member.getLoginId();
        this.name = member.getName();
        this.email = member.getEmail();
        this.role = member.getRole();
        this.active = member.isActive();
        this.createdAt = member.getCreatedAt();
    }

    public static MemberResponse from(Member member) {
        return new MemberResponse(member);
    }
}
