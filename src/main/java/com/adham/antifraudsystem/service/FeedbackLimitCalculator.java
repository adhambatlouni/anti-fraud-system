package com.adham.antifraudsystem.service;

import com.adham.antifraudsystem.model.FraudTypes.TransactionResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FeedbackLimitCalculator {

    private static final BigDecimal CURRENT_LIMIT_WEIGHT = new BigDecimal("0.8");
    private static final BigDecimal TRANSACTION_WEIGHT = new BigDecimal("0.2");

    public AdjustedLimits calculate(
            TransactionResult original,
            TransactionResult feedback,
            long maxAllowed,
            long maxManual,
            long amount) {

        return switch (original) {
            case ALLOWED -> switch (feedback) {
                case MANUAL_PROCESSING ->
                        new AdjustedLimits(decrease(maxAllowed, amount), maxManual);
                case PROHIBITED -> new AdjustedLimits(
                        decrease(maxAllowed, amount),
                        decrease(maxManual, amount)
                );
                default -> throw sameResult();
            };
            case MANUAL_PROCESSING -> switch (feedback) {
                case ALLOWED ->
                        new AdjustedLimits(increase(maxAllowed, amount), maxManual);
                case PROHIBITED ->
                        new AdjustedLimits(maxAllowed, decrease(maxManual, amount));
                default -> throw sameResult();
            };
            case PROHIBITED -> switch (feedback) {
                case ALLOWED -> new AdjustedLimits(
                        increase(maxAllowed, amount),
                        increase(maxManual, amount)
                );
                case MANUAL_PROCESSING ->
                        new AdjustedLimits(maxAllowed, increase(maxManual, amount));
                default -> throw sameResult();
            };
        };
    }

    private IllegalArgumentException sameResult() {
        return new IllegalArgumentException(
                "Feedback must differ from the original result"
        );
    }

    private long increase(long currentLimit, long amount) {
        return weighted(currentLimit, amount, true);
    }

    private long decrease(long currentLimit, long amount) {
        return weighted(currentLimit, amount, false);
    }

    private long weighted(long currentLimit, long amount, boolean increasing) {
        BigDecimal currentPart = BigDecimal.valueOf(currentLimit)
                .multiply(CURRENT_LIMIT_WEIGHT);
        BigDecimal transactionPart = BigDecimal.valueOf(amount)
                .multiply(TRANSACTION_WEIGHT);

        BigDecimal result = increasing
                ? currentPart.add(transactionPart)
                : currentPart.subtract(transactionPart);

        return result.setScale(0, RoundingMode.CEILING).longValueExact();
    }

    public record AdjustedLimits(long maxAllowed, long maxManual) {
    }
}
