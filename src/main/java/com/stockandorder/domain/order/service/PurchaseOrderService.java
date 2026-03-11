package com.stockandorder.domain.order.service;

import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.repository.MemberRepository;
import com.stockandorder.domain.order.dto.PurchaseOrderCreateRequest;
import com.stockandorder.domain.order.dto.PurchaseOrderListResponse;
import com.stockandorder.domain.order.dto.PurchaseOrderResponse;
import com.stockandorder.domain.order.dto.PurchaseOrderSearchCondition;
import com.stockandorder.domain.order.entity.PurchaseOrder;
import com.stockandorder.domain.order.entity.PurchaseOrderItem;
import com.stockandorder.domain.order.repository.PurchaseOrderRepository;
import com.stockandorder.domain.product.entity.Product;
import com.stockandorder.domain.product.repository.ProductRepository;
import com.stockandorder.domain.supplier.entity.Supplier;
import com.stockandorder.domain.supplier.enums.SupplierType;
import com.stockandorder.domain.supplier.repository.SupplierRepository;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderService {

    private static final int MAX_RETRY = 3;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    public Long createOrder(PurchaseOrderCreateRequest request, Long requesterId) {
        Supplier supplier = findSupplier(request.getSupplierId());
        validateSupplierForPurchase(supplier);

        Member requester = findMember(requesterId);

        String orderNumber = generateOrderNumber();
        PurchaseOrder order = PurchaseOrder.create(orderNumber, supplier, requester, request.getNote());

        for (PurchaseOrderCreateRequest.ItemRequest itemReq : request.getItems()) {
            Product product = findProduct(itemReq.getProductId());
            PurchaseOrderItem item = PurchaseOrderItem.create(
                    product,
                    itemReq.getQuantity(),
                    product.getUnitPrice()
            );
            order.addItem(item);
        }

        PurchaseOrder saved = saveWithRetry(order);
        return saved.getOrderId();
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrderListResponse> searchOrders(PurchaseOrderSearchCondition condition, Pageable pageable) {
        return purchaseOrderRepository.search(condition, pageable);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getOrder(Long orderId) {
        PurchaseOrder order = findById(orderId);
        return PurchaseOrderResponse.from(order);
    }

    public void approveOrder(Long orderId, Long approverId) {
        PurchaseOrder order = findById(orderId);
        Member approver = findMember(approverId);
        validateNotSelfApproval(order, approverId);
        order.approve(approver);
    }

    public void rejectOrder(Long orderId, Long approverId, String rejectReason) {
        PurchaseOrder order = findById(orderId);
        Member approver = findMember(approverId);
        validateNotSelfApproval(order, approverId);
        order.reject(approver, rejectReason);
    }

    public void cancelOrder(Long orderId, Long requesterId) {
        PurchaseOrder order = findById(orderId);
        validateRequester(order, requesterId);
        order.cancel();
    }

    private void validateNotSelfApproval(PurchaseOrder order, Long approverId) {
        if (order.getRequester().getMemberId().equals(approverId)) {
            throw new BusinessException(ErrorCode.ORDER_SELF_APPROVAL);
        }
    }

    private void validateRequester(PurchaseOrder order, Long requesterId) {
        if (!order.getRequester().getMemberId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_REQUESTER);
        }
    }

    private String generateOrderNumber() {
        String prefix = "PO-" + LocalDate.now().format(DATE_FORMAT) + "-";
        return purchaseOrderRepository.findMaxOrderNumberByPrefix(prefix)
                .map(max -> {
                    int seq = Integer.parseInt(max.substring(max.lastIndexOf("-") + 1));
                    return prefix + String.format("%03d", seq + 1);
                })
                .orElse(prefix + "001");
    }

    private PurchaseOrder saveWithRetry(PurchaseOrder order) {
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            try {
                PurchaseOrder saved = purchaseOrderRepository.save(order);
                purchaseOrderRepository.flush();
                return saved;
            } catch (DataIntegrityViolationException e) {
                if (attempt == MAX_RETRY - 1) {
                    throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
                }
                String newOrderNumber = generateOrderNumber();
                order.changeOrderNumber(newOrderNumber);
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private void validateSupplierForPurchase(Supplier supplier) {
        if (!supplier.isActive()) {
            throw new BusinessException(ErrorCode.SUPPLIER_INACTIVE);
        }
        if (supplier.getSupplierType() == SupplierType.SALES) {
            throw new BusinessException(ErrorCode.SUPPLIER_TYPE_INVALID);
        }
    }

    private PurchaseOrder findById(Long orderId) {
        return purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    private Supplier findSupplier(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUPPLIER_NOT_FOUND));
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
