package com.stockandorder.domain.member.service;

import com.stockandorder.domain.member.dto.MemberCreateRequest;
import com.stockandorder.domain.member.dto.MemberResponse;
import com.stockandorder.domain.member.dto.MemberUpdateRequest;
import com.stockandorder.domain.member.dto.PasswordChangeRequest;
import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.repository.MemberRepository;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<MemberResponse> getMembers(Pageable pageable) {
        return memberRepository.findAll(pageable).map(MemberResponse::from);
    }

    @Transactional(readOnly = true)
    public MemberResponse getMember(Long memberId) {
        return MemberResponse.from(findById(memberId));
    }

    public void createMember(MemberCreateRequest request) {
        if (memberRepository.existsByLoginId(request.getLoginId())) {
            throw new BusinessException(ErrorCode.MEMBER_LOGIN_ID_DUPLICATE);
        }
        Member member = Member.create(
                request.getLoginId(),
                passwordEncoder.encode(request.getPassword()),
                request.getName(),
                request.getEmail(),
                request.getRole()
        );
        memberRepository.save(member);
    }

    public void updateMember(Long memberId, Long actorId, MemberUpdateRequest request) {
        Member member = findById(memberId);
        if (member.getRole() != request.getRole()) {
            validateNotSelfOrProtected(member, memberId, actorId);
        }
        member.updateProfile(request.getName(), request.getEmail(), request.getRole());
    }

    public void deactivateMember(Long memberId, Long actorId) {
        Member member = findById(memberId);
        validateNotSelfOrProtected(member, memberId, actorId);
        member.deactivate();
    }

    public void activateMember(Long memberId) {
        Member member = findById(memberId);
        member.activate();
    }

    public void changePassword(Long memberId, PasswordChangeRequest request) {
        Member member = findById(memberId);
        if (member.isDemoAccount()) {
            throw new BusinessException(ErrorCode.MEMBER_DEMO_PASSWORD_LOCKED);
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.MEMBER_PASSWORD_MISMATCH);
        }
        member.changePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    // ADMIN 계정이 스스로를 잠그거나 초기 관리자를 잠가 관리 권한이 사라지는 것을 막는다.
    // 데모 계정은 "본인" 검사만으로는 부족하다. 데모 계정으로 새 ADMIN을 만들어 그 계정으로 잠그면
    // 우회되므로, 누가 요청하든 막는다.
    // 본인 여부는 로그인 정보가 필요해 엔티티가 아닌 서비스에서 판단한다.
    private void validateNotSelfOrProtected(Member member, Long memberId, Long actorId) {
        if (memberId.equals(actorId)) {
            throw new BusinessException(ErrorCode.MEMBER_SELF_MODIFICATION_NOT_ALLOWED);
        }
        if (member.isInitialAdmin()) {
            throw new BusinessException(ErrorCode.MEMBER_INITIAL_ADMIN_PROTECTED);
        }
        if (member.isDemoAccount()) {
            throw new BusinessException(ErrorCode.MEMBER_DEMO_ACCOUNT_PROTECTED);
        }
    }

    private Member findById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
