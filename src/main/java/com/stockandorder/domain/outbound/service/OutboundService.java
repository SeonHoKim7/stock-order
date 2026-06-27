package com.stockandorder.domain.outbound.service;

import com.stockandorder.domain.outbound.dto.OutboundCreateRequest;
import com.stockandorder.domain.outbound.dto.OutboundListResponse;
import com.stockandorder.domain.outbound.dto.OutboundResponse;
import com.stockandorder.domain.outbound.dto.OutboundSearchCondition;
import com.stockandorder.domain.outbound.entity.Outbound;
import com.stockandorder.domain.outbound.repository.OutboundRepository;
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
public class OutboundService {

    private static final int MAX_RETRY = 3;

    private final OutboundProcessor outboundProcessor;
    private final OutboundRepository outboundRepository;

    /**
     * 출고 등록. 충돌 시 새 트랜잭션으로 재시도한다.
     * 입고와 달리 낙관적 락이 없으므로 재시도 대상은 출고번호 UNIQUE 충돌(DataIntegrityViolationException)뿐이다.
     * 재고 경합은 StockService의 비관적 락이 대기로 처리하므로 재시도가 필요 없다.
     */
    public Long createOutbound(OutboundCreateRequest request, Long processorId) {
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            try {
                return outboundProcessor.createOnce(request, processorId);
            } catch (DataIntegrityViolationException e) {
                if (attempt == MAX_RETRY - 1) {
                    throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
                }
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @Transactional(readOnly = true)
    public Page<OutboundListResponse> searchOutbounds(OutboundSearchCondition condition, Pageable pageable) {
        return outboundRepository.search(condition, pageable);
    }

    @Transactional(readOnly = true)
    public OutboundResponse getOutbound(Long outboundId) {
        Outbound outbound = outboundRepository.findDetailById(outboundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OUTBOUND_NOT_FOUND));
        return OutboundResponse.from(outbound);
    }
}
