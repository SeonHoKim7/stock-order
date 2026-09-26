package com.stockandorder.domain.product.service;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.category.repository.CategoryRepository;
import com.stockandorder.domain.product.dto.ProductCreateRequest;
import com.stockandorder.domain.product.dto.ProductResponse;
import com.stockandorder.domain.product.dto.ProductUpdateRequest;
import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.domain.product.repository.ProductRepository;
import com.stockandorder.domain.stock.entity.Stock;
import com.stockandorder.domain.stock.repository.StockRepository;
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
    private final StockRepository stockRepository;

    /**
     * 활성 상품만 조회한다. 발주·출고 등록 폼의 상품 선택지가 이 메서드를 쓰므로,
     * 비활성 상품이 섞이지 않는 쪽이 기본값이어야 한다.
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String keyword, Long categoryId, Pageable pageable) {
        return searchProducts(keyword, categoryId, false, pageable);
    }

    /**
     * @param includeInactive true면 비활성 상품까지 함께 반환한다. 상품 관리 화면에서
     *                        비활성화한 상품을 다시 찾아 활성화하려면 목록에 나타나야 하므로 필요하다.
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String keyword, Long categoryId,
                                                boolean includeInactive, Pageable pageable) {
        // 빈 문자열은 null로 처리해 전체 조회
        String kw = (keyword != null && keyword.isBlank()) ? null : keyword;
        return productRepository.search(kw, categoryId, includeInactive, pageable)
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
        Product product = productRepository.save(Product.create(
                request.getProductCode(),
                request.getName(),
                category,
                request.getUnit(),
                request.getPurchasePrice(),
                request.getSalePrice(),
                request.getSafetyStock(),
                request.getDescription()
        ));
        // Stock 항상 존재 불변식 유지. 상품 생성과 동시에 quantity=0 재고 레코드 생성
        stockRepository.save(Stock.create(product));
    }

    public void updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = findById(productId);
        Category category = resolveCategory(request.getCategoryId());
        product.update(
                request.getName(),
                category,
                request.getUnit(),
                request.getPurchasePrice(),
                request.getSalePrice(),
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
