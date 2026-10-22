package com.adham.antifraudsystem.service;

import com.adham.antifraudsystem.dto.TransactionDtos.TransactionHistoryResponse;
import com.adham.antifraudsystem.dto.TransactionDtos.TransactionRequest;
import com.adham.antifraudsystem.validation.CardValidator;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TransactionHistoryService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

    private static final RowMapper<TransactionHistoryResponse> HISTORY_MAPPER =
            (rs, rowNum) -> new TransactionHistoryResponse(
                    rs.getLong("id"),
                    rs.getLong("amount"),
                    rs.getString("ip"),
                    rs.getString("number"),
                    rs.getString("region"),
                    rs.getTimestamp("transaction_date")
                            .toLocalDateTime()
                            .format(DATE_FORMATTER),
                    rs.getString("result"),
                    rs.getString("feedback")
            );

    private final JdbcTemplate jdbcTemplate;

    public TransactionHistoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveTransaction(
            TransactionRequest request,
            LocalDateTime transactionDate,
            FraudDecisionEngine.Decision decision,
            FraudDecisionEngine.Signals signals) {

        jdbcTemplate.update(
                """
                INSERT INTO transaction_history
                    (amount, ip, number, region, transaction_date,
                     result, feedback, decision_reasons,
                     applied_max_allowed, applied_max_manual,
                     suspicious_ip_at_decision, stolen_card_at_decision,
                     different_ip_count, different_region_count, rule_version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                request.amount(),
                request.ip(),
                request.number(),
                request.region(),
                Timestamp.valueOf(transactionDate),
                decision.result().name(),
                "",
                decision.info(),
                signals.maxAllowed(),
                signals.maxManual(),
                signals.suspiciousIp(),
                signals.stolenCard(),
                signals.differentIpCount(),
                signals.differentRegionCount(),
                FraudDecisionEngine.RULE_VERSION
        );
    }

    public long countDifferentIps(
            String cardNumber,
            String currentIp,
            LocalDateTime transactionDate) {

        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(DISTINCT ip)
                FROM transaction_history
                WHERE number = ?
                  AND ip <> ?
                  AND transaction_date BETWEEN ? AND ?
                """,
                Long.class,
                cardNumber,
                currentIp,
                Timestamp.valueOf(transactionDate.minusHours(1)),
                Timestamp.valueOf(transactionDate)
        );

        return count == null ? 0 : count;
    }

    public long countDifferentRegions(
            String cardNumber,
            String currentRegion,
            LocalDateTime transactionDate) {

        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(DISTINCT region)
                FROM transaction_history
                WHERE number = ?
                  AND region <> ?
                  AND transaction_date BETWEEN ? AND ?
                """,
                Long.class,
                cardNumber,
                currentRegion,
                Timestamp.valueOf(transactionDate.minusHours(1)),
                Timestamp.valueOf(transactionDate)
        );

        return count == null ? 0 : count;
    }

    public List<TransactionHistoryResponse> getAllHistory() {

        return jdbcTemplate.query(
                """
                SELECT id, amount, ip, number, region,
                       transaction_date, result, feedback
                FROM transaction_history
                ORDER BY id ASC
                """,
                HISTORY_MAPPER
        );
    }

    public List<TransactionHistoryResponse> getHistoryByCard(
            String number) {

        if (!CardValidator.isValid(number)) {
            throw new IllegalArgumentException(
                    "Invalid card number"
            );
        }

        List<TransactionHistoryResponse> transactions =
                jdbcTemplate.query(
                        """
                        SELECT id, amount, ip, number, region,
                               transaction_date, result, feedback
                        FROM transaction_history
                        WHERE number = ?
                        ORDER BY id ASC
                        """,
                        HISTORY_MAPPER,
                        number
                );

        if (transactions.isEmpty()) {
            throw new NoSuchElementException(
                    "No transactions found for this card"
            );
        }

        return transactions;
    }

    // Retrieve and lock one transaction for feedback processing.
    public TransactionHistoryResponse getTransactionForFeedback(
            long transactionId) {

        return jdbcTemplate.queryForObject(
                """
                SELECT id, amount, ip, number, region,
                       transaction_date, result, feedback
                FROM transaction_history
                WHERE id = ?
                FOR UPDATE
                """,
                HISTORY_MAPPER,
                transactionId
        );
    }

    // Record the specialist's feedback.
    public void saveFeedback(long transactionId, String feedback) {

        jdbcTemplate.update(
                """
                UPDATE transaction_history
                SET feedback = ?
                WHERE id = ?
                """,
                feedback,
                transactionId
        );
    }
}
