package com.stockandorder.domain.order.service;

import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.repository.MemberRepository;
import com.stockandorder.domain.order.dto.PurchaseOrderCreateRequest;
import com.stockandorder.domain.order.dto.PurchaseOrderListResponse;
import com.stockandorder.domain.order.dto.PurchaseOrderResponse;
import com.stockandorder.domain.order.dto.PurchaseOrderSearchCondition;
import com.stockandorder.domain.order.entity.PurchaseOrder;
import com.stockandorder.domain.order.enums.OrderStatus;
import com.stockandorder.domain.order.repository.PurchaseOrderRepository;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private static final int MAX_RETRY = 3;

    private final PurchaseOrderProcessor purchaseOrderProcessor;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final MemberRepository memberRepository;

    /**
     * 발주 등록. 발주번호 UNIQUE 충돌 시 새 트랜잭션으로 재시도한다.
     *
     * 이 메서드에는 트랜잭션을 걸지 않는다. 충돌이 난 트랜잭션은 rollback-only로 마킹되어
     * 같은 트랜잭션 안에서는 다시 시도할 수 없기 때문에, 재시도는 반드시 트랜잭션 경계 밖에서
     * 이루어져야 한다. 매 시도는 PurchaseOrderProcessor.createOnce가 새 트랜잭션으로 수행하며,
     * 번호도 그 안에서 다시 채번된다. 저경합이라 재시도는 드물고, 한도 초과 시 실패로 처리한다.
     */
    public Long createOrder(PurchaseOrderCreateRequest request, Long requesterId) {
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            try {
                return purchaseOrderProcessor.createOnce(request, requesterId);
            } catch (DataIntegrityViolationException e) {
                if (attempt == MAX_RETRY - 1) {
                    throw new BusinessException(ErrorCode.CONCURRENCY_RETRY_EXHAUSTED);
                }
            }
        }
        throw new BusinessException(ErrorCode.CONCURRENCY_RETRY_EXHAUSTED);
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

    /**
     * 승인 대기(PENDING) 발주 건수(대시보드 위젯용). 입고/출고의 "오늘"과 달리 시점 경계가 없는 현재 상태 집계다.
     */
    @Transactional(readOnly = true)
    public long countPending() {
        return purchaseOrderRepository.countByStatus(OrderStatus.PENDING);
    }

    @Transactional
    public void approveOrder(Long orderId, Long approverId) {
        PurchaseOrder order = findById(orderId);
        Member approver = findMember(approverId);
        validateNotSelfApproval(order, approverId);
        order.approve(approver);
    }

    @Transactional
    public void rejectOrder(Long orderId, Long approverId, String rejectReason) {
        PurchaseOrder order = findById(orderId);
        Member approver = findMember(approverId);
        validateNotSelfApproval(order, approverId);
        order.reject(approver, rejectReason);
    }

    @Transactional
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

    private PurchaseOrder findById(Long orderId) {
        return purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
