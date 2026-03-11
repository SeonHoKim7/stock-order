package com.stockandorder.domain.order.controller;

import com.stockandorder.domain.order.dto.PurchaseOrderCreateRequest;
import com.stockandorder.domain.order.dto.PurchaseOrderSearchCondition;
import com.stockandorder.domain.order.service.PurchaseOrderService;
import com.stockandorder.domain.product.service.ProductService;
import com.stockandorder.domain.supplier.service.SupplierService;
import com.stockandorder.global.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final SupplierService supplierService;
    private final ProductService productService;

    @GetMapping
    public String list(@ModelAttribute("condition") PurchaseOrderSearchCondition condition,
                       @PageableDefault(size = 10, sort = "orderedAt", direction = Sort.Direction.DESC) Pageable pageable,
                       Model model) {
        model.addAttribute("orders", purchaseOrderService.searchOrders(condition, pageable));
        model.addAttribute("suppliers", supplierService.getActiveSuppliers());
        return "order/list";
    }

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

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @AuthenticationPrincipal CustomUserDetails userDetails,
                          RedirectAttributes redirectAttributes) {
        purchaseOrderService.approveOrder(id, userDetails.getMemberId());
        redirectAttributes.addFlashAttribute("message", "발주가 승인되었습니다.");
        return "redirect:/orders/" + id;
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam String rejectReason,
                         @AuthenticationPrincipal CustomUserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        purchaseOrderService.rejectOrder(id, userDetails.getMemberId(), rejectReason);
        redirectAttributes.addFlashAttribute("message", "발주가 반려되었습니다.");
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @AuthenticationPrincipal CustomUserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        purchaseOrderService.cancelOrder(id, userDetails.getMemberId());
        redirectAttributes.addFlashAttribute("message", "발주가 취소되었습니다.");
        return "redirect:/orders/" + id;
    }
}
