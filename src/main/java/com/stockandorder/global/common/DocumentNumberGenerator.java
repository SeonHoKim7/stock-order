package com.stockandorder.global.common;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Function;

/**
 * 발주/입고/출고 문서번호를 생성한다. 형식은 {접두사}-{yyyyMMdd}-{당일 일련번호 3자리}.
 *
 * [왜 지금 공통화했나]
 * 발주(PO) 구현 시점에는 같은 형식을 쓸 문서가 하나뿐이라 성급한 추상화를 피해 각 서비스에 두었다.
 * 입고(IN)·출고(OUT)까지 만들고 나서야 세 곳이 접두사만 다르고 완전히 동일하다는 것이 확인되어
 * 여기로 모았다. 형식이 갈라질 가능성은 접두사를 파라미터로 받는 선에서만 열어둔다.
 *
 * [당일 최대 번호 조회를 파라미터로 받는 이유]
 * 문서마다 조회 대상 테이블이 다르므로(purchase_order/inbound/outbound), 이 클래스가 리포지토리
 * 세 개를 알게 하는 대신 "접두사로 당일 최대 번호를 찾는 방법"만 함수로 넘겨받는다.
 * 번호 생성 규칙만 이 클래스가 소유하고, 어디서 찾을지는 호출하는 도메인이 소유한다.
 *
 * [동시성]
 * 조회와 저장 사이에 다른 트랜잭션이 같은 번호를 선점할 수 있다. 이는 여기서 막지 않고
 * DB의 UNIQUE 제약으로 감지한 뒤 호출 측이 새 트랜잭션으로 재시도해 해결한다.
 * B2B 내부 시스템의 저경합 환경을 전제한 선택이며, 경합이 커지면 채번 전략 자체를 교체한다.
 */
@Component
public class DocumentNumberGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String DELIMITER = "-";
    private static final String SEQUENCE_FORMAT = "%03d";
    private static final int FIRST_SEQUENCE = 1;

    /**
     * @param documentPrefix   문서 종류 접두사 (예: "PO", "IN", "OUT")
     * @param maxNumberFinder  접두사로 시작하는 당일 최대 번호를 찾는 조회 (예: repository::findMaxOrderNumberByPrefix)
     */
    public String generate(String documentPrefix, Function<String, Optional<String>> maxNumberFinder) {
        String prefix = documentPrefix + DELIMITER + LocalDate.now().format(DATE_FORMAT) + DELIMITER;
        int sequence = maxNumberFinder.apply(prefix)
                .map(this::nextSequenceOf)
                .orElse(FIRST_SEQUENCE);
        return prefix + String.format(SEQUENCE_FORMAT, sequence);
    }

    private int nextSequenceOf(String maxNumber) {
        String sequencePart = maxNumber.substring(maxNumber.lastIndexOf(DELIMITER) + 1);
        return Integer.parseInt(sequencePart) + 1;
    }
}
