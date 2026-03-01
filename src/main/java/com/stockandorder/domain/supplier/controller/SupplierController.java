package com.stockandorder.domain.supplier.controller;

import com.stockandorder.domain.supplier.dto.SupplierCreateRequest;
import com.stockandorder.domain.supplier.dto.SupplierResponse;
import com.stockandorder.domain.supplier.dto.SupplierUpdateRequest;
import com.stockandorder.domain.supplier.enums.SupplierType;
import com.stockandorder.domain.supplier.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) SupplierType supplierType,
                       @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                       Model model) {
        Page<SupplierResponse> suppliers = supplierService.searchSuppliers(keyword, supplierType, pageable);
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("supplierTypes", SupplierType.values());
        model.addAttribute("keyword", keyword);
        model.addAttribute("supplierType", supplierType);
        return "supplier/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("supplier", supplierService.getSupplier(id));
        return "supplier/detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String createForm(Model model) {
        model.addAttribute("form", new SupplierCreateRequest());
        model.addAttribute("supplierTypes", SupplierType.values());
        return "supplier/create-form";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String create(@Valid @ModelAttribute("form") SupplierCreateRequest form,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("supplierTypes", SupplierType.values());
            return "supplier/create-form";
        }
        supplierService.createSupplier(form);
        return "redirect:/suppliers";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String editForm(@PathVariable Long id, Model model) {
        SupplierResponse supplier = supplierService.getSupplier(id);
        SupplierUpdateRequest form = new SupplierUpdateRequest();
        form.setName(supplier.getName());
        form.setSupplierType(supplier.getSupplierType());
        form.setContactName(supplier.getContactName());
        form.setContactPhone(supplier.getContactPhone());
        form.setContactEmail(supplier.getContactEmail());
        form.setAddress(supplier.getAddress());
        model.addAttribute("supplier", supplier);
        model.addAttribute("form", form);
        model.addAttribute("supplierTypes", SupplierType.values());
        return "supplier/edit-form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") SupplierUpdateRequest form,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("supplier", supplierService.getSupplier(id));
            model.addAttribute("supplierTypes", SupplierType.values());
            return "supplier/edit-form";
        }
        supplierService.updateSupplier(id, form);
        return "redirect:/suppliers/" + id;
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        supplierService.deactivateSupplier(id);
        redirectAttributes.addFlashAttribute("message", "거래처가 비활성화되었습니다.");
        return "redirect:/suppliers/" + id;
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public String activate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        supplierService.activateSupplier(id);
        redirectAttributes.addFlashAttribute("message", "거래처가 활성화되었습니다.");
        return "redirect:/suppliers/" + id;
    }
}
