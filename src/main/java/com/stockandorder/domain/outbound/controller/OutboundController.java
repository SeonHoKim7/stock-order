package com.stockandorder.domain.outbound.controller;

import com.stockandorder.domain.outbound.dto.OutboundCreateRequest;
import com.stockandorder.domain.outbound.dto.OutboundSearchCondition;
import com.stockandorder.domain.outbound.service.OutboundService;
import com.stockandorder.domain.product.service.ProductService;
import com.stockandorder.domain.supplier.service.SupplierService;
import com.stockandorder.global.auth.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
@RequestMapping("/outbounds")
public class OutboundController {

    private final OutboundService outboundService;
    private final SupplierService supplierService;
    private final ProductService productService;

    @GetMapping
    public String list(@ModelAttribute("condition") OutboundSearchCondition condition,
                       @PageableDefault(size = 10) Pageable pageable,
                       Model model) {
        model.addAttribute("outbounds", outboundService.searchOutbounds(condition, pageable));
        model.addAttribute("suppliers", supplierService.getActiveSuppliers());
        return "outbound/list";
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new OutboundCreateRequest());
        model.addAttribute("suppliers", supplierService.getActiveSuppliers());
        model.addAttribute("products", productService.searchProducts(
                null, null, PageRequest.of(0, 1000, Sort.by("name"))).getContent());
        return "outbound/create-form";
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @PostMapping
    public String create(@Valid @ModelAttribute("form") OutboundCreateRequest form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails userDetails,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("suppliers", supplierService.getActiveSuppliers());
            model.addAttribute("products", productService.searchProducts(
                    null, null, PageRequest.of(0, 1000, Sort.by("name"))).getContent());
            return "outbound/create-form";
        }
        Long outboundId = outboundService.createOutbound(form, userDetails.getMemberId());
        redirectAttributes.addFlashAttribute("message", "출고가 등록되었습니다.");
        return "redirect:/outbounds/" + outboundId;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("outbound", outboundService.getOutbound(id));
        return "outbound/detail";
    }
}
