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

        memberService.updateMember(1L, 2L, request);

        // 더티체킹으로 save() 없이 변경 → 엔티티 상태 직접 검증
        assertThat(member.getName()).isEqualTo("홍길순");
        assertThat(member.getEmail()).isEqualTo("new@test.com");
        assertThat(member.getRole()).isEqualTo(Role.MANAGER);
    }

    @Test
    @DisplayName("존재하지 않는 회원 수정 시 MEMBER_NOT_FOUND 예외가 발생한다")
    void updateMember_notFound_throwsException() {
        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.updateMember(999L, 2L, new MemberUpdateRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    @DisplayName("deactivateMember() 호출 시 isActive가 false가 된다")
    void deactivateMember_setsIsActiveFalse() {
        Member member = Member.create("staff01", "encodedPw", "홍길동", null, Role.STAFF);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        memberService.deactivateMember(1L, 2L);

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

    @Test
    @DisplayName("본인 계정을 비활성화하면 MEMBER_SELF_MODIFICATION_NOT_ALLOWED 예외가 발생한다")
    void deactivateMember_self_throwsException() {
        Member member = Member.create("testAdmin", "encodedPw", "데모 관리자", null, Role.ADMIN);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.deactivateMember(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_SELF_MODIFICATION_NOT_ALLOWED));
        assertThat(member.isActive()).isTrue();
    }

    @Test
    @DisplayName("초기 관리자 계정을 비활성화하면 MEMBER_INITIAL_ADMIN_PROTECTED 예외가 발생한다")
    void deactivateMember_initialAdmin_throwsException() {
        Member admin = Member.create(Member.INITIAL_ADMIN_LOGIN_ID, "encodedPw", "관리자", null, Role.ADMIN);
        given(memberRepository.findById(1L)).willReturn(Optional.of(admin));

        assertThatThrownBy(() -> memberService.deactivateMember(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_INITIAL_ADMIN_PROTECTED));
        assertThat(admin.isActive()).isTrue();
    }

    @Test
    @DisplayName("본인 계정의 역할을 변경하면 MEMBER_SELF_MODIFICATION_NOT_ALLOWED 예외가 발생한다")
    void updateMember_selfRoleChange_throwsException() {
        Member member = Member.create("testAdmin", "encodedPw", "데모 관리자", null, Role.ADMIN);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.updateMember(1L, 1L, updateRequest("데모 관리자", Role.STAFF)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_SELF_MODIFICATION_NOT_ALLOWED));
        assertThat(member.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("초기 관리자 계정의 역할을 변경하면 MEMBER_INITIAL_ADMIN_PROTECTED 예외가 발생한다")
    void updateMember_initialAdminRoleChange_throwsException() {
        Member admin = Member.create(Member.INITIAL_ADMIN_LOGIN_ID, "encodedPw", "관리자", null, Role.ADMIN);
        given(memberRepository.findById(1L)).willReturn(Optional.of(admin));

        assertThatThrownBy(() -> memberService.updateMember(1L, 2L, updateRequest("관리자", Role.STAFF)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_INITIAL_ADMIN_PROTECTED));
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("역할을 바꾸지 않는 본인 정보 수정은 허용된다")
    void updateMember_selfWithoutRoleChange_updatesProfile() {
        Member member = Member.create("testAdmin", "encodedPw", "데모 관리자", null, Role.ADMIN);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        memberService.updateMember(1L, 1L, updateRequest("데모", Role.ADMIN));

        assertThat(member.getName()).isEqualTo("데모");
    }

    @Test
    @DisplayName("데모 계정의 비밀번호를 변경하면 MEMBER_DEMO_PASSWORD_LOCKED 예외가 발생한다")
    void changePassword_demoAccount_throwsException() {
        Member demo = Member.create(Member.DEMO_LOGIN_ID, "encodedOldPw", "데모 관리자", null, Role.ADMIN);
        given(memberRepository.findById(1L)).willReturn(Optional.of(demo));

        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("oldPw");
        request.setNewPassword("newPw123");

        assertThatThrownBy(() -> memberService.changePassword(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_DEMO_PASSWORD_LOCKED));
        assertThat(demo.getPassword()).isEqualTo("encodedOldPw");
    }

    private MemberUpdateRequest updateRequest(String name, Role role) {
        MemberUpdateRequest request = new MemberUpdateRequest();
        request.setName(name);
        request.setRole(role);
        return request;
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
