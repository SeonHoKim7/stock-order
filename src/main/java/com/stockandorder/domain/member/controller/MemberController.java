package com.stockandorder.domain.member.controller;

import com.stockandorder.domain.member.dto.MemberCreateRequest;
import com.stockandorder.domain.member.dto.MemberResponse;
import com.stockandorder.domain.member.dto.MemberUpdateRequest;
import com.stockandorder.domain.member.dto.PasswordChangeRequest;
import com.stockandorder.domain.member.enums.Role;
import com.stockandorder.domain.member.service.MemberService;
import com.stockandorder.global.auth.CustomUserDetails;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/admin/members")
    public String memberList(Model model,
                             @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<MemberResponse> members = memberService.getMembers(pageable);
        model.addAttribute("members", members);
        return "member/list";
    }

    @GetMapping("/admin/members/new")
    public String createForm(Model model) {
        model.addAttribute("form", new MemberCreateRequest());
        model.addAttribute("roles", Role.values());
        return "member/create-form";
    }

    @PostMapping("/admin/members")
    public String create(@Valid @ModelAttribute("form") MemberCreateRequest form,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", Role.values());
            return "member/create-form";
        }
        try {
            memberService.createMember(form);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.MEMBER_LOGIN_ID_DUPLICATE) {
                bindingResult.rejectValue("loginId", "duplicate", e.getMessage());
                model.addAttribute("roles", Role.values());
                return "member/create-form";
            }
            throw e;
        }
        return "redirect:/admin/members";
    }

    @GetMapping("/admin/members/{id}")
    public String memberDetail(@PathVariable Long id, Model model) {
        model.addAttribute("member", memberService.getMember(id));
        return "member/detail";
    }

    @GetMapping("/admin/members/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        MemberResponse member = memberService.getMember(id);
        MemberUpdateRequest form = new MemberUpdateRequest();
        form.setName(member.getName());
        form.setEmail(member.getEmail());
        form.setRole(member.getRole());
        model.addAttribute("member", member);
        model.addAttribute("form", form);
        model.addAttribute("roles", Role.values());
        return "member/edit-form";
    }

    @PostMapping("/admin/members/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") MemberUpdateRequest form,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("member", memberService.getMember(id));
            model.addAttribute("roles", Role.values());
            return "member/edit-form";
        }
        memberService.updateMember(id, form);
        return "redirect:/admin/members/" + id;
    }

    @PostMapping("/admin/members/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        memberService.deactivateMember(id);
        redirectAttributes.addFlashAttribute("message", "계정이 비활성화되었습니다.");
        return "redirect:/admin/members/" + id;
    }

    @PostMapping("/admin/members/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        memberService.activateMember(id);
        redirectAttributes.addFlashAttribute("message", "계정이 활성화되었습니다.");
        return "redirect:/admin/members/" + id;
    }

    @GetMapping("/my/profile")
    public String myProfile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("member", memberService.getMember(userDetails.getMemberId()));
        model.addAttribute("passwordForm", new PasswordChangeRequest());
        return "my/profile";
    }

    @PostMapping("/my/password")
    public String changePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 @Valid @ModelAttribute("passwordForm") PasswordChangeRequest form,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("member", memberService.getMember(userDetails.getMemberId()));
            return "my/profile";
        }
        memberService.changePassword(userDetails.getMemberId(), form);
        redirectAttributes.addFlashAttribute("message", "비밀번호가 변경되었습니다.");
        return "redirect:/my/profile";
    }
}
