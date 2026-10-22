package com.adham.antifraudsystem.service;

import com.adham.antifraudsystem.model.FraudTypes.TransactionResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudLimitService {

    private final JdbcTemplate jdbcTemplate;
    private final FeedbackLimitCalculator calculator;

    public FraudLimitService(
            JdbcTemplate jdbcTemplate,
            FeedbackLimitCalculator calculator) {

        this.jdbcTemplate = jdbcTemplate;
        this.calculator = calculator;
    }

    public record Limits(
            long maxAllowed,
            long maxManual
    ) {
    }

    public record LimitChange(
            Limits before,
            Limits after
    ) {
    }

    public Limits getCurrentLimits() {

        return jdbcTemplate.queryForObject(
                """
                SELECT max_allowed, max_manual
                FROM transaction_limits
                WHERE id = 1
                """,
                (rs, rowNum) -> new Limits(
                        rs.getLong("max_allowed"),
                        rs.getLong("max_manual")
                )
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public LimitChange applyFeedback(
            TransactionResult originalResult,
            TransactionResult feedback,
            long amount) {

        Limits limits = jdbcTemplate.queryForObject(
                """
                SELECT max_allowed, max_manual
                FROM transaction_limits
                WHERE id = 1
                FOR UPDATE
                """,
                (rs, rowNum) -> new Limits(
                        rs.getLong("max_allowed"),
                        rs.getLong("max_manual")
                )
        );

        FeedbackLimitCalculator.AdjustedLimits adjusted = calculator.calculate(
                originalResult,
                feedback,
                limits.maxAllowed(),
                limits.maxManual(),
                amount
        );

        jdbcTemplate.update(
                """
                UPDATE transaction_limits
                SET max_allowed = ?, max_manual = ?
                WHERE id = 1
                """,
                adjusted.maxAllowed(),
                adjusted.maxManual()
        );

        return new LimitChange(
                limits,
                new Limits(adjusted.maxAllowed(), adjusted.maxManual())
        );
    }
}
