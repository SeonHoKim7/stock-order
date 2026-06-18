package com.stockandorder.domain.inbound.service;

import com.stockandorder.domain.inbound.dto.InboundCreateRequest;
import com.stockandorder.domain.inbound.dto.InboundListResponse;
import com.stockandorder.domain.inbound.dto.InboundResponse;
import com.stockandorder.domain.inbound.dto.InboundSearchCondition;
import com.stockandorder.domain.inbound.entity.Inbound;
import com.stockandorder.domain.inbound.repository.InboundRepository;
import com.stockandorder.global.exception.BusinessException;
import com.stockandorder.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InboundService {

    private static final int MAX_RETRY = 3;

    private final InboundProcessor inboundProcessor;
    private final InboundRepository inboundRepository;

    /**
     * 입고 등록. 충돌 시 새 트랜잭션으로 재시도한다(트랜잭션 경계 밖에서 재시도해야 깨끗하게 다시 시도 가능).
     * - 발주 낙관적 락 충돌(OptimisticLockingFailureException, E-1): 같은 발주 동시 입고
     * - 입고번호 UNIQUE 충돌(DataIntegrityViolationException, F-1): 번호를 새로 생성해 재시도
     * 저경합이라 재시도는 드물게 일어나며, 한도 초과 시 실패로 처리한다.
     */
    public Long createInbound(InboundCreateRequest request, Long processorId) {
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            try {
                return inboundProcessor.createOnce(request, processorId);
            } catch (OptimisticLockingFailureException | DataIntegrityViolationException e) {
                if (attempt == MAX_RETRY - 1) {
                    throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
                }
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @Transactional(readOnly = true)
    public Page<InboundListResponse> searchInbounds(InboundSearchCondition condition, Pageable pageable) {
        return inboundRepository.search(condition, pageable);
    }

    @Transactional(readOnly = true)
    public InboundResponse getInbound(Long inboundId) {
        Inbound inbound = inboundRepository.findDetailById(inboundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INBOUND_NOT_FOUND));
        return InboundResponse.from(inbound);
    }
}
