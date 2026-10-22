package com.adham.antifraudsystem.service;

import com.adham.antifraudsystem.model.FraudTypes.TransactionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class FeedbackLimitCalculatorTest {

    private final FeedbackLimitCalculator calculator = new FeedbackLimitCalculator();

    @ParameterizedTest(name = "{0} corrected to {1}")
    @MethodSource("validFeedbackTransitions")
    void adjustsOnlyTheLimitsAffectedByFeedback(
            TransactionResult original,
            TransactionResult feedback,
            long expectedAllowed,
            long expectedManual) {

        FeedbackLimitCalculator.AdjustedLimits result = calculator.calculate(
                original,
                feedback,
                200,
                1_500,
                210
        );

        assertThat(result).isEqualTo(
                new FeedbackLimitCalculator.AdjustedLimits(
                        expectedAllowed,
                        expectedManual
                )
        );
    }

    @Test
    void roundsFractionalLimitsTowardPositiveInfinity() {
        FeedbackLimitCalculator.AdjustedLimits result = calculator.calculate(
                TransactionResult.MANUAL_PROCESSING,
                TransactionResult.ALLOWED,
                201,
                1_500,
                202
        );

        assertThat(result.maxAllowed()).isEqualTo(202);
    }

    @Test
    void rejectsFeedbackMatchingTheOriginalResult() {
        assertThatIllegalArgumentException().isThrownBy(() -> calculator.calculate(
                TransactionResult.ALLOWED,
                TransactionResult.ALLOWED,
                200,
                1_500,
                100
        ));
    }

    private static Stream<Arguments> validFeedbackTransitions() {
        return Stream.of(
                Arguments.of(TransactionResult.ALLOWED,
                        TransactionResult.MANUAL_PROCESSING, 118, 1_500),
                Arguments.of(TransactionResult.ALLOWED,
                        TransactionResult.PROHIBITED, 118, 1_158),
                Arguments.of(TransactionResult.MANUAL_PROCESSING,
                        TransactionResult.ALLOWED, 202, 1_500),
                Arguments.of(TransactionResult.MANUAL_PROCESSING,
                        TransactionResult.PROHIBITED, 200, 1_158),
                Arguments.of(TransactionResult.PROHIBITED,
                        TransactionResult.ALLOWED, 202, 1_242),
                Arguments.of(TransactionResult.PROHIBITED,
                        TransactionResult.MANUAL_PROCESSING, 200, 1_242)
        );
    }
}
