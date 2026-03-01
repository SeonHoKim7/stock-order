package com.stockandorder.domain.member.service;

import com.stockandorder.domain.member.dto.MemberCreateRequest;
import com.stockandorder.domain.member.dto.MemberUpdateRequest;
import com.stockandorder.domain.member.dto.PasswordChangeRequest;
import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import com.stockandorder.domain.member.repository.MemberRepository;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("중복되지 않는 loginId로 회원을 생성하면 저장된다")
    void createMember_uniqueLoginId_savesMember() {
        MemberCreateRequest request = createRequest("newuser", "password123", Role.STAFF);
        given(memberRepository.existsByLoginId("newuser")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encodedPw");

        memberService.createMember(request);

        then(memberRepository).should().save(any(Member.class));
    }

    @Test
    @DisplayName("중복된 loginId로 회원 생성 시 MEMBER_LOGIN_ID_DUPLICATE 예외가 발생한다")
    void createMember_duplicateLoginId_throwsException() {
        MemberCreateRequest request = createRequest("duplicate", "password123", Role.STAFF);
        given(memberRepository.existsByLoginId("duplicate")).willReturn(true);

        assertThatThrownBy(() -> memberService.createMember(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_LOGIN_ID_DUPLICATE));
    }

    @Test
    @DisplayName("회원 정보 수정 시 name, email, role이 변경된다")
    void updateMember_validRequest_updatesProfile() {
        Member member = Member.create("staff01", "encodedPw", "홍길동", null, Role.STAFF);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        MemberUpdateRequest request = new MemberUpdateRequest();
        request.setName("홍길순");
        request.setEmail("new@test.com");
        request.setRole(Role.MANAGER);

        memberService.updateMember(1L, request);

        // 더티체킹으로 save() 없이 변경 → 엔티티 상태 직접 검증
        assertThat(member.getName()).isEqualTo("홍길순");
        assertThat(member.getEmail()).isEqualTo("new@test.com");
        assertThat(member.getRole()).isEqualTo(Role.MANAGER);
    }

    @Test
    @DisplayName("존재하지 않는 회원 수정 시 MEMBER_NOT_FOUND 예외가 발생한다")
    void updateMember_notFound_throwsException() {
        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.updateMember(999L, new MemberUpdateRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    @DisplayName("deactivateMember() 호출 시 isActive가 false가 된다")
    void deactivateMember_setsIsActiveFalse() {
        Member member = Member.create("staff01", "encodedPw", "홍길동", null, Role.STAFF);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        memberService.deactivateMember(1L);

        assertThat(member.isActive()).isFalse();
    }

    @Test
    @DisplayName("activateMember() 호출 시 isActive가 true가 된다")
    void activateMember_setsIsActiveTrue() {
        Member member = Member.create("staff01", "encodedPw", "홍길동", null, Role.STAFF);
        member.deactivate();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        memberService.activateMember(1L);

        assertThat(member.isActive()).isTrue();
    }

    @Test
    @DisplayName("현재 비밀번호가 일치하면 새 비밀번호로 변경된다")
    void changePassword_correctCurrentPassword_changesPassword() {
        Member member = Member.create("staff01", "encodedOldPw", "홍길동", null, Role.STAFF);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(passwordEncoder.matches("oldPw", "encodedOldPw")).willReturn(true);
        given(passwordEncoder.encode("newPw123")).willReturn("encodedNewPw");

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("oldPw");
        request.setNewPassword("newPw123");

        memberService.changePassword(1L, request);

        assertThat(member.getPassword()).isEqualTo("encodedNewPw");
    }

    @Test
    @DisplayName("현재 비밀번호가 불일치하면 MEMBER_PASSWORD_MISMATCH 예외가 발생한다")
    void changePassword_wrongCurrentPassword_throwsException() {
        Member member = Member.create("staff01", "encodedOldPw", "홍길동", null, Role.STAFF);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrongPw", "encodedOldPw")).willReturn(false);

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("wrongPw");
        request.setNewPassword("newPw123");

        assertThatThrownBy(() -> memberService.changePassword(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_PASSWORD_MISMATCH));
    }

    private MemberCreateRequest createRequest(String loginId, String password, Role role) {
        MemberCreateRequest request = new MemberCreateRequest();
        request.setLoginId(loginId);
        request.setPassword(password);
        request.setName("테스트유저");
        request.setRole(role);
        return request;
    }
}
