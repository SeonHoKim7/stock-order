package com.stockandorder.domain.order.controller;

import com.stockandorder.domain.order.dto.PurchaseOrderCreateRequest;
import com.stockandorder.domain.order.service.PurchaseOrderService;
import com.stockandorder.domain.product.service.ProductService;
import com.stockandorder.domain.supplier.service.SupplierService;
import com.stockandorder.global.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final SupplierService supplierService;
    private final ProductService productService;

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new PurchaseOrderCreateRequest());
        return "order/create-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") PurchaseOrderCreateRequest form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails userDetails,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "order/create-form";
        }
        Long orderId = purchaseOrderService.createOrder(form, userDetails.getMemberId());
        redirectAttributes.addFlashAttribute("message", "발주가 등록되었습니다.");
        return "redirect:/orders/" + orderId;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("order", purchaseOrderService.getOrder(id));
        return "order/detail";
    }
}
