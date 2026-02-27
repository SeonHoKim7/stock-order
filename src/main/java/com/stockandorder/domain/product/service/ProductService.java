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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String keyword, Long categoryId, Pageable pageable) {
        // 빈 문자열은 null로 처리해 전체 조회
        String kw = (keyword != null && keyword.isBlank()) ? null : keyword;
        return productRepository.search(kw, categoryId, pageable)
                .map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long productId) {
        return ProductResponse.from(findById(productId));
    }

    public void createProduct(ProductCreateRequest request) {
        if (productRepository.existsByProductCode(request.getProductCode())) {
            throw new BusinessException(ErrorCode.PRODUCT_CODE_DUPLICATE);
        }
        Category category = resolveCategory(request.getCategoryId());
        productRepository.save(Product.create(
                request.getProductCode(),
                request.getName(),
                category,
                request.getUnit(),
                request.getUnitPrice(),
                request.getSafetyStock(),
                request.getDescription()
        ));
    }

    public void updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = findById(productId);
        Category category = resolveCategory(request.getCategoryId());
        product.update(
                request.getName(),
                category,
                request.getUnit(),
                request.getUnitPrice(),
                request.getSafetyStock(),
                request.getDescription()
        );
    }

    public void deactivateProduct(Long productId) {
        findById(productId).deactivate();
    }

    public void activateProduct(Long productId) {
        findById(productId).activate();
    }

    private Product findById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private Category resolveCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}
