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

    public void updateMember(Long memberId, MemberUpdateRequest request) {
        Member member = findById(memberId);
        member.updateProfile(request.getName(), request.getEmail(), request.getRole());
    }

    public void deactivateMember(Long memberId) {
        Member member = findById(memberId);
        member.deactivate();
    }

    public void activateMember(Long memberId) {
        Member member = findById(memberId);
        member.activate();
    }

    public void changePassword(Long memberId, PasswordChangeRequest request) {
        Member member = findById(memberId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.MEMBER_PASSWORD_MISMATCH);
        }
        member.changePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    private Member findById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
