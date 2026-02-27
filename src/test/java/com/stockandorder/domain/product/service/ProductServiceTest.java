package com.stockandorder.domain.product.service;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.category.repository.CategoryRepository;
import com.stockandorder.domain.product.dto.ProductCreateRequest;
import com.stockandorder.domain.product.dto.ProductResponse;
import com.stockandorder.domain.product.dto.ProductUpdateRequest;
import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.domain.product.repository.ProductRepository;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    // ============================================================
    // searchProducts
    // ============================================================

    @Test
    @DisplayName("빈 문자열 키워드는 null로 변환하여 repository에 전달한다")
    void searchProducts_blankKeyword_passesNullToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        given(productRepository.search(null, null, pageable)).willReturn(Page.empty());

        productService.searchProducts("   ", null, pageable);

        then(productRepository).should().search(null, null, pageable);
    }

    @Test
    @DisplayName("null 키워드는 그대로 null로 repository에 전달한다")
    void searchProducts_nullKeyword_passesNullToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        given(productRepository.search(null, null, pageable)).willReturn(Page.empty());

        productService.searchProducts(null, null, pageable);

        then(productRepository).should().search(null, null, pageable);
    }

    @Test
    @DisplayName("검색 결과를 ProductResponse 페이지로 변환하여 반환한다")
    void searchProducts_returnsPageOfProductResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        Category category = Category.create("미분류", null);
        Product product = Product.create("PRD-00001", "노트북", category, "EA",
                BigDecimal.valueOf(1200000), 3, null);
        given(productRepository.search(null, null, pageable))
                .willReturn(new PageImpl<>(List.of(product)));

        Page<ProductResponse> result = productService.searchProducts(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProductCode()).isEqualTo("PRD-00001");
    }

    // ============================================================
    // getProduct
    // ============================================================

    @Test
    @DisplayName("존재하는 상품 조회 시 ProductResponse를 반환한다")
    void getProduct_existingProduct_returnsResponse() {
        Category category = Category.create("전자기기", null);
        Product product = Product.create("PRD-00001", "노트북", category, "EA",
                BigDecimal.valueOf(1200000), 3, "업무용");
        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        ProductResponse result = productService.getProduct(1L);

        assertThat(result.getProductCode()).isEqualTo("PRD-00001");
        assertThat(result.getName()).isEqualTo("노트북");
        assertThat(result.getCategoryName()).isEqualTo("전자기기");
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 PRODUCT_NOT_FOUND 예외가 발생한다")
    void getProduct_notFound_throwsException() {
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND));
    }

    // ============================================================
    // createProduct
    // ============================================================

    @Test
    @DisplayName("카테고리와 함께 상품을 정상 등록하면 저장된다")
    void createProduct_withCategory_savesProduct() {
        ProductCreateRequest request = createRequest("PRD-00001", "노트북", 1L);
        Category category = Category.create("전자기기", null);
        given(productRepository.existsByProductCode("PRD-00001")).willReturn(false);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));

        productService.createProduct(request);

        then(productRepository).should().save(any(Product.class));
    }

    @Test
    @DisplayName("중복된 상품코드로 등록 시 PRODUCT_CODE_DUPLICATE 예외가 발생한다")
    void createProduct_duplicateCode_throwsException() {
        ProductCreateRequest request = createRequest("PRD-00001", "노트북", 1L);
        given(productRepository.existsByProductCode("PRD-00001")).willReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PRODUCT_CODE_DUPLICATE));
    }

    @Test
    @DisplayName("존재하지 않는 카테고리ID 지정 시 CATEGORY_NOT_FOUND 예외가 발생한다")
    void createProduct_categoryNotFound_throwsException() {
        ProductCreateRequest request = createRequest("PRD-00001", "노트북", 999L);
        given(productRepository.existsByProductCode("PRD-00001")).willReturn(false);
        given(categoryRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND));
    }

    // ============================================================
    // updateProduct
    // ============================================================

    @Test
    @DisplayName("상품 수정 시 전달한 값으로 필드가 변경된다")
    void updateProduct_validRequest_updatesFields() {
        Category oldCategory = Category.create("미분류", null);
        Product product = Product.create("PRD-00001", "노트북", oldCategory, "EA",
                BigDecimal.valueOf(1000000), 3, "구설명");
        Category newCategory = Category.create("사무용품", null);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(categoryRepository.findById(2L)).willReturn(Optional.of(newCategory));

        ProductUpdateRequest request = updateRequest("노트북 Pro", 2L, "BOX",
                BigDecimal.valueOf(1500000), 5, "신설명");
        productService.updateProduct(1L, request);

        assertThat(product.getName()).isEqualTo("노트북 Pro");
        assertThat(product.getCategory()).isEqualTo(newCategory);
        assertThat(product.getUnit()).isEqualTo("BOX");
        assertThat(product.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(1500000));
        assertThat(product.getSafetyStock()).isEqualTo(5);
        assertThat(product.getDescription()).isEqualTo("신설명");
    }

    @Test
    @DisplayName("존재하지 않는 상품 수정 시 PRODUCT_NOT_FOUND 예외가 발생한다")
    void updateProduct_notFound_throwsException() {
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(999L, new ProductUpdateRequest()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Test
    @DisplayName("수정 시 존재하지 않는 카테고리ID 지정 시 CATEGORY_NOT_FOUND 예외가 발생한다")
    void updateProduct_categoryNotFound_throwsException() {
        Category category = Category.create("미분류", null);
        Product product = Product.create("PRD-00001", "노트북", category, "EA",
                BigDecimal.valueOf(1000000), 0, null);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(categoryRepository.findById(999L)).willReturn(Optional.empty());

        ProductUpdateRequest request = updateRequest("노트북", 999L, "EA",
                BigDecimal.valueOf(1000000), 0, null);

        assertThatThrownBy(() -> productService.updateProduct(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND));
    }

    // ============================================================
    // 상품 비활성화(deactivateProduct)
    // ============================================================

    @Test
    @DisplayName("deactivateProduct() 호출 시 isActive가 false가 된다")
    void deactivateProduct_setsIsActiveFalse() {
        Category category = Category.create("미분류", null);
        Product product = Product.create("PRD-00001", "노트북", category, "EA",
                BigDecimal.valueOf(1000000), 0, null);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        productService.deactivateProduct(1L);

        assertThat(product.isActive()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 상품 비활성화 시 PRODUCT_NOT_FOUND 예외가 발생한다")
    void deactivateProduct_notFound_throwsException() {
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deactivateProduct(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND));
    }

    // ============================================================
    // 상품 활성화(activateProduct)
    // ============================================================

    @Test
    @DisplayName("activateProduct() 호출 시 isActive가 true가 된다")
    void activateProduct_setsIsActiveTrue() {
        Category category = Category.create("미분류", null);
        Product product = Product.create("PRD-00001", "노트북", category, "EA",
                BigDecimal.valueOf(1000000), 0, null);
        product.deactivate();
        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        productService.activateProduct(1L);

        assertThat(product.isActive()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 상품 활성화 시 PRODUCT_NOT_FOUND 예외가 발생한다")
    void activateProduct_notFound_throwsException() {
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.activateProduct(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND));
    }

    // ============================================================
    // 헬퍼 메서드
    // ============================================================

    private ProductCreateRequest createRequest(String code, String name, Long categoryId) {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setProductCode(code);
        request.setName(name);
        request.setCategoryId(categoryId);
        request.setUnit("EA");
        request.setUnitPrice(BigDecimal.valueOf(10000));
        request.setSafetyStock(0);
        return request;
    }

    private ProductUpdateRequest updateRequest(String name, Long categoryId, String unit,
                                               BigDecimal unitPrice, int safetyStock, String description) {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName(name);
        request.setCategoryId(categoryId);
        request.setUnit(unit);
        request.setUnitPrice(unitPrice);
        request.setSafetyStock(safetyStock);
        request.setDescription(description);
        return request;
    }
}
