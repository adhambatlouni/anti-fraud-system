package com.adham.antifraudsystem.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CardValidatorTest {

    @Test
    void masksAllButTheLastFourDigits() {
        assertThat(CardValidator.mask("4000008449433403"))
                .isEqualTo("************3403");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "4000008449433403",
            "4000000000000002"
    })
    void acceptsSixteenDigitNumbersWithValidLuhnChecksums(String number) {
        assertThat(CardValidator.isValid(number)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "400000844943340",
            "40000084494334030",
            "4000008449433404",
            "40000084494334A3",
            " 4000008449433403"
    })
    void rejectsNumbersWithInvalidLengthCharactersOrChecksum(String number) {
        assertThat(CardValidator.isValid(number)).isFalse();
    }
}
