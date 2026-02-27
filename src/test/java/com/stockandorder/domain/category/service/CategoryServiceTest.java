package com.stockandorder.domain.category.service;

import com.stockandorder.domain.category.dto.CategoryRequest;
import com.stockandorder.domain.category.dto.CategoryResponse;
import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.category.repository.CategoryRepository;
import com.stockandorder.domain.product.repository.ProductRepository;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    // ============================================================
    // getCategories
    // ============================================================

    @Test
    @DisplayName("카테고리 목록을 이름 오름차순으로 반환한다")
    void getCategories_returnsSortedList() {
        given(categoryRepository.findAll(Sort.by("name")))
                .willReturn(List.of(
                        Category.create("과일", null),
                        Category.create("채소", null)
                ));

        List<CategoryResponse> result = categoryService.getCategories();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("과일");
        assertThat(result.get(1).getName()).isEqualTo("채소");
    }

    // ============================================================
    // createCategory
    // ============================================================

    @Test
    @DisplayName("중복되지 않는 이름으로 카테고리를 생성하면 저장된다")
    void createCategory_uniqueName_savesCategory() {
        given(categoryRepository.existsByName("전자제품")).willReturn(false);

        categoryService.createCategory(request("전자제품", "전자 관련 상품"));

        then(categoryRepository).should().save(any(Category.class));
    }

    @Test
    @DisplayName("중복된 이름으로 카테고리 생성 시 CATEGORY_NAME_DUPLICATE 예외가 발생한다")
    void createCategory_duplicateName_throwsException() {
        given(categoryRepository.existsByName("전자제품")).willReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request("전자제품", null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CATEGORY_NAME_DUPLICATE));
    }

    // ============================================================
    // updateCategory
    // ============================================================

    @Test
    @DisplayName("수정 시 이름과 설명이 변경된다")
    void updateCategory_validRequest_updatesFields() {
        Category category = Category.create("전자제품", "구설명");
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(categoryRepository.existsByNameAndCategoryIdNot("가전제품", 1L)).willReturn(false);

        categoryService.updateCategory(1L, request("가전제품", "신설명"));

        assertThat(category.getName()).isEqualTo("가전제품");
        assertThat(category.getDescription()).isEqualTo("신설명");
    }

    @Test
    @DisplayName("수정 시 다른 카테고리와 이름이 중복되면 CATEGORY_NAME_DUPLICATE 예외가 발생한다")
    void updateCategory_duplicateNameWithOther_throwsException() {
        Category category = Category.create("전자제품", null);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(categoryRepository.existsByNameAndCategoryIdNot("가전제품", 1L)).willReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(1L, request("가전제품", null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CATEGORY_NAME_DUPLICATE));
    }

    @Test
    @DisplayName("자기 자신의 이름과 동일하게 수정하면 중복 예외가 발생하지 않는다")
    void updateCategory_sameNameAsSelf_doesNotThrow() {
        Category category = Category.create("전자제품", "설명");
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(categoryRepository.existsByNameAndCategoryIdNot("전자제품", 1L)).willReturn(false);

        categoryService.updateCategory(1L, request("전자제품", "새설명"));

        assertThat(category.getDescription()).isEqualTo("새설명");
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 수정 시 CATEGORY_NOT_FOUND 예외가 발생한다")
    void updateCategory_notFound_throwsException() {
        given(categoryRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(999L, request("이름", null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND));
    }

    // ============================================================
    // deleteCategory
    // ============================================================

    @Test
    @DisplayName("상품이 없는 카테고리를 삭제하면 delete가 호출된다")
    void deleteCategory_noProducts_callsDelete() {
        Category category = Category.create("전자제품", null);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(productRepository.existsByCategoryCategoryId(1L)).willReturn(false);

        categoryService.deleteCategory(1L);

        then(categoryRepository).should().delete(category);
    }

    @Test
    @DisplayName("상품이 존재하는 카테고리 삭제 시 CATEGORY_HAS_PRODUCTS 예외가 발생한다")
    void deleteCategory_hasProducts_throwsException() {
        Category category = Category.create("전자제품", null);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(productRepository.existsByCategoryCategoryId(1L)).willReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CATEGORY_HAS_PRODUCTS));
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 삭제 시 CATEGORY_NOT_FOUND 예외가 발생한다")
    void deleteCategory_notFound_throwsException() {
        given(categoryRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND));
    }

    // ============================================================
    // 헬퍼 메서드
    // ============================================================

    private CategoryRequest request(String name, String description) {
        CategoryRequest request = new CategoryRequest();
        request.setName(name);
        request.setDescription(description);
        return request;
    }
}
