package com.stockandorder.domain.category.controller;

import com.stockandorder.domain.category.dto.CategoryRequest;
import com.stockandorder.domain.category.dto.CategoryResponse;
import com.stockandorder.domain.category.service.CategoryService;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    // ============================================================
    // 목록 + 등록 폼 (한 페이지)
    // ============================================================

    @GetMapping
    public String categoryList(Model model) {
        model.addAttribute("categories", categoryService.getCategories());
        model.addAttribute("createForm", new CategoryRequest());
        return "category/list";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String create(@Valid @ModelAttribute("createForm") CategoryRequest form,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.getCategories());
            return "category/list";
        }
        try {
            categoryService.createCategory(form);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.CATEGORY_NAME_DUPLICATE) {
                bindingResult.rejectValue("name", "duplicate", e.getMessage());
                model.addAttribute("categories", categoryService.getCategories());
                return "category/list";
            }
            throw e;
        }
        return "redirect:/categories";
    }

    // ============================================================
    // 수정
    // ============================================================

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String editForm(@PathVariable Long id, Model model) {
        CategoryResponse category = categoryService.getCategory(id);
        CategoryRequest form = new CategoryRequest();
        form.setName(category.getName());
        form.setDescription(category.getDescription());
        model.addAttribute("category", category);
        model.addAttribute("editForm", form);
        return "category/edit-form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("editForm") CategoryRequest form,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("category", categoryService.getCategory(id));
            return "category/edit-form";
        }
        try {
            categoryService.updateCategory(id, form);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.CATEGORY_NAME_DUPLICATE) {
                bindingResult.rejectValue("name", "duplicate", e.getMessage());
                model.addAttribute("category", categoryService.getCategory(id));
                return "category/edit-form";
            }
            throw e;
        }
        return "redirect:/categories";
    }

    // ============================================================
    // 삭제
    // ============================================================

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        categoryService.deleteCategory(id);
        redirectAttributes.addFlashAttribute("message", "카테고리가 삭제되었습니다.");
        return "redirect:/categories";
    }
}
