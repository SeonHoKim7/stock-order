package com.stockandorder.domain.category.service;

import com.stockandorder.domain.category.dto.CategoryRequest;
import com.stockandorder.domain.category.dto.CategoryResponse;
import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.category.repository.CategoryRepository;
import com.stockandorder.domain.product.repository.ProductRepository;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll(Sort.by("name")).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long categoryId) {
        return CategoryResponse.from(findById(categoryId));
    }

    public void createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_DUPLICATE);
        }
        categoryRepository.save(Category.create(request.getName(), request.getDescription()));
    }

    public void updateCategory(Long categoryId, CategoryRequest request) {
        Category category = findById(categoryId);
        if (categoryRepository.existsByNameAndCategoryIdNot(request.getName(), categoryId)) {
            throw new BusinessException(ErrorCode.CATEGORY_NAME_DUPLICATE);
        }
        category.update(request.getName(), request.getDescription());
    }

    public void deleteCategory(Long categoryId) {
        Category category = findById(categoryId);
        if (productRepository.existsByCategoryCategoryId(categoryId)) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_PRODUCTS);
        }
        categoryRepository.delete(category);
    }

    private Category findById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}
