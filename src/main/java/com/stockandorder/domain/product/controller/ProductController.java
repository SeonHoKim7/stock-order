package com.stockandorder.domain.product.controller;

import com.stockandorder.domain.category.service.CategoryService;
import com.stockandorder.domain.product.dto.ProductCreateRequest;
import com.stockandorder.domain.product.dto.ProductResponse;
import com.stockandorder.domain.product.dto.ProductUpdateRequest;
import com.stockandorder.domain.product.service.ProductService;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
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
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long categoryId,
                       @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                       Model model) {
        Page<ProductResponse> products = productService.searchProducts(keyword, categoryId, pageable);
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getCategories());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        return "product/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.getProduct(id));
        return "product/detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String createForm(Model model) {
        model.addAttribute("form", new ProductCreateRequest());
        model.addAttribute("categories", categoryService.getCategories());
        return "product/create-form";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String create(@Valid @ModelAttribute("form") ProductCreateRequest form,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.getCategories());
            return "product/create-form";
        }
        try {
            productService.createProduct(form);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.PRODUCT_CODE_DUPLICATE) {
                bindingResult.rejectValue("productCode", "duplicate", e.getMessage());
                model.addAttribute("categories", categoryService.getCategories());
                return "product/create-form";
            }
            throw e;
        }
        return "redirect:/products";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String editForm(@PathVariable Long id, Model model) {
        ProductResponse product = productService.getProduct(id);
        ProductUpdateRequest form = new ProductUpdateRequest();
        form.setName(product.getName());
        form.setCategoryId(product.getCategoryId());
        form.setUnit(product.getUnit());
        form.setUnitPrice(product.getUnitPrice());
        form.setSafetyStock(product.getSafetyStock());
        form.setDescription(product.getDescription());
        model.addAttribute("product", product);
        model.addAttribute("form", form);
        model.addAttribute("categories", categoryService.getCategories());
        return "product/edit-form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") ProductUpdateRequest form,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("product", productService.getProduct(id));
            model.addAttribute("categories", categoryService.getCategories());
            return "product/edit-form";
        }
        productService.updateProduct(id, form);
        return "redirect:/products/" + id;
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.deactivateProduct(id);
        redirectAttributes.addFlashAttribute("message", "상품이 비활성화되었습니다.");
        return "redirect:/products/" + id;
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public String activate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.activateProduct(id);
        redirectAttributes.addFlashAttribute("message", "상품이 활성화되었습니다.");
        return "redirect:/products/" + id;
    }
}
