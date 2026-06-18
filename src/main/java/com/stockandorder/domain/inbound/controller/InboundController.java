package com.stockandorder.domain.inbound.controller;

import com.stockandorder.domain.inbound.dto.InboundCreateRequest;
import com.stockandorder.domain.inbound.dto.InboundSearchCondition;
import com.stockandorder.domain.inbound.service.InboundService;
import com.stockandorder.domain.supplier.service.SupplierService;
import com.stockandorder.global.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/inbounds")
public class InboundController {

    private final InboundService inboundService;
    private final SupplierService supplierService;

    @GetMapping
    public String list(@ModelAttribute("condition") InboundSearchCondition condition,
                       @PageableDefault(size = 10) Pageable pageable,
                       Model model) {
        model.addAttribute("inbounds", inboundService.searchInbounds(condition, pageable));
        model.addAttribute("suppliers", supplierService.getActiveSuppliers());
        return "inbound/list";
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new InboundCreateRequest());
        return "inbound/create-form";
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @PostMapping
    public String create(@Valid @ModelAttribute("form") InboundCreateRequest form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "inbound/create-form";
        }
        Long inboundId = inboundService.createInbound(form, userDetails.getMemberId());
        redirectAttributes.addFlashAttribute("message", "입고가 등록되었습니다.");
        return "redirect:/inbounds/" + inboundId;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("inbound", inboundService.getInbound(id));
        return "inbound/detail";
    }
}
