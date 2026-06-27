package com.stockandorder.domain.inbound.controller;

import com.stockandorder.domain.inbound.dto.InboundCreateRequest;
import com.stockandorder.domain.inbound.dto.InboundSearchCondition;
import com.stockandorder.domain.inbound.service.InboundService;
import com.stockandorder.domain.order.dto.PurchaseOrderListResponse;
import com.stockandorder.domain.order.dto.PurchaseOrderSearchCondition;
import com.stockandorder.domain.order.enums.OrderStatus;
import com.stockandorder.domain.order.service.PurchaseOrderService;
import com.stockandorder.domain.supplier.service.SupplierService;
import com.stockandorder.global.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/inbounds")
public class InboundController {

    private final InboundService inboundService;
    private final SupplierService supplierService;
    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    public String list(@ModelAttribute("condition") InboundSearchCondition condition,
                       @PageableDefault(size = 10) Pageable pageable,
                       Model model) {
        model.addAttribute("inbounds", inboundService.searchInbounds(condition, pageable));
        model.addAttribute("suppliers", supplierService.getActiveSuppliers());
        return "inbound/list";
    }

    /**
     * 입고 등록 폼. 2단계로 동작한다.
     * - orderId 없이 진입: 입고 가능한 발주 목록만 보여주고 발주 선택을 유도한다.
     * - orderId 지정: 해당 발주의 미완료 항목을 입고 입력 줄로 펼쳐 보여준다.
     */
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Long orderId, Model model) {
        InboundCreateRequest form = new InboundCreateRequest();
        form.setInboundDate(LocalDate.now());
        form.setOrderId(orderId);
        model.addAttribute("form", form);
        addFormReferenceData(model, orderId);
        return "inbound/create-form";
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @PostMapping
    public String create(@Valid @ModelAttribute("form") InboundCreateRequest form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails userDetails,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model, form.getOrderId());
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

    // 등록 폼 데이터: 입고 가능한 발주 목록 + (발주가 선택된 경우) 그 발주의 항목들.
    private void addFormReferenceData(Model model, Long orderId) {
        model.addAttribute("receivableOrders", receivableOrders());
        if (orderId != null) {
            model.addAttribute("order", purchaseOrderService.getOrder(orderId));
        }
    }

    // 입고 가능한 발주 = 승인(APPROVED) 또는 진행중(IN_PROGRESS). 두 상태를 각각 조회해 합친다.
    private List<PurchaseOrderListResponse> receivableOrders() {
        Pageable pageable = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "orderedAt"));
        List<PurchaseOrderListResponse> result = new ArrayList<>();
        for (OrderStatus status : List.of(OrderStatus.APPROVED, OrderStatus.IN_PROGRESS)) {
            PurchaseOrderSearchCondition condition = new PurchaseOrderSearchCondition();
            condition.setStatus(status);
            result.addAll(purchaseOrderService.searchOrders(condition, pageable).getContent());
        }
        return result;
    }
}
