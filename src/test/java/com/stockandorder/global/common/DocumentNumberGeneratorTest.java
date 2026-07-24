package com.stockandorder.global.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentNumberGeneratorTest {

    private final DocumentNumberGenerator generator = new DocumentNumberGenerator();

    private String today() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    @Test
    @DisplayName("당일 문서가 없으면 001번으로 시작한다")
    void generate_noDocumentToday_startsFromOne() {
        String result = generator.generate("PO", prefix -> Optional.empty());

        assertThat(result).isEqualTo("PO-" + today() + "-001");
    }

    @Test
    @DisplayName("당일 최대 번호가 있으면 그 다음 번호를 발급한다")
    void generate_existingNumber_incrementsSequence() {
        String result = generator.generate("IN", prefix -> Optional.of("IN-" + today() + "-005"));

        assertThat(result).isEqualTo("IN-" + today() + "-006");
    }

    @Test
    @DisplayName("일련번호는 3자리로 0을 채운다")
    void generate_padsSequenceToThreeDigits() {
        String result = generator.generate("OUT", prefix -> Optional.of("OUT-" + today() + "-008"));

        assertThat(result).endsWith("-009");
    }

    @Test
    @DisplayName("일련번호가 999를 넘으면 자리수가 늘어난다(당일 1000건 이상)")
    void generate_beyondThreeDigits_growsWidth() {
        String result = generator.generate("PO", prefix -> Optional.of("PO-" + today() + "-999"));

        assertThat(result).endsWith("-1000");
    }

    @Test
    @DisplayName("조회에는 접두사와 오늘 날짜로 만든 prefix가 그대로 전달된다")
    void generate_passesPrefixToFinder() {
        String[] captured = new String[1];

        generator.generate("IN", prefix -> {
            captured[0] = prefix;
            return Optional.empty();
        });

        assertThat(captured[0]).isEqualTo("IN-" + today() + "-");
    }
}
