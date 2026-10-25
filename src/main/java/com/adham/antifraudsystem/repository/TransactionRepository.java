package com.adham.antifraudsystem.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class TransactionRepository {

    private static final RowMapper<StoredTransaction> TRANSACTION_MAPPER =
            (rs, rowNum) -> new StoredTransaction(
                    rs.getLong("id"),
                    rs.getLong("amount"),
                    rs.getString("ip"),
                    rs.getString("number"),
                    rs.getString("region"),
                    rs.getTimestamp("transaction_date").toLocalDateTime(),
                    rs.getString("result"),
                    rs.getString("feedback")
            );

    private final JdbcTemplate jdbcTemplate;

    public TransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(NewTransaction transaction) {
        jdbcTemplate.update(
                """
                INSERT INTO transaction_history
                    (amount, ip, number, region, transaction_date,
                     result, feedback, decision_reasons,
                     applied_max_allowed, applied_max_manual,
                     suspicious_ip_at_decision, stolen_card_at_decision,
                     different_ip_count, different_region_count, rule_version)
                VALUES (?, ?, ?, ?, ?, ?, '', ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                transaction.amount(),
                transaction.ip(),
                transaction.number(),
                transaction.region(),
                Timestamp.valueOf(transaction.date()),
                transaction.result(),
                transaction.reasons(),
                transaction.maxAllowed(),
                transaction.maxManual(),
                transaction.suspiciousIp(),
                transaction.stolenCard(),
                transaction.differentIpCount(),
                transaction.differentRegionCount(),
                transaction.ruleVersion()
        );
    }

    public long countDifferentIps(
            String cardNumber,
            String currentIp,
            LocalDateTime transactionDate) {

        return countDistinct(
                """
                SELECT COUNT(DISTINCT ip)
                FROM transaction_history
                WHERE number = ?
                  AND ip <> ?
                  AND transaction_date BETWEEN ? AND ?
                """,
                cardNumber,
                currentIp,
                transactionDate
        );
    }

    public long countDifferentRegions(
            String cardNumber,
            String currentRegion,
            LocalDateTime transactionDate) {

        return countDistinct(
                """
                SELECT COUNT(DISTINCT region)
                FROM transaction_history
                WHERE number = ?
                  AND region <> ?
                  AND transaction_date BETWEEN ? AND ?
                """,
                cardNumber,
                currentRegion,
                transactionDate
        );
    }

    public List<StoredTransaction> findAll() {
        return jdbcTemplate.query(
                selectHistory() + " ORDER BY id ASC",
                TRANSACTION_MAPPER
        );
    }

    public List<StoredTransaction> findByCard(String number) {
        return jdbcTemplate.query(
                selectHistory() + " WHERE number = ? ORDER BY id ASC",
                TRANSACTION_MAPPER,
                number
        );
    }

    public StoredTransaction findByIdForUpdate(long transactionId) {
        return jdbcTemplate.queryForObject(
                selectHistory() + " WHERE id = ? FOR UPDATE",
                TRANSACTION_MAPPER,
                transactionId
        );
    }

    public void updateFeedback(long transactionId, String feedback) {
        jdbcTemplate.update(
                "UPDATE transaction_history SET feedback = ? WHERE id = ?",
                feedback,
                transactionId
        );
    }

    private long countDistinct(
            String sql,
            String cardNumber,
            String currentValue,
            LocalDateTime transactionDate) {

        Long count = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                cardNumber,
                currentValue,
                Timestamp.valueOf(transactionDate.minusHours(1)),
                Timestamp.valueOf(transactionDate)
        );
        return count == null ? 0 : count;
    }

    private String selectHistory() {
        return """
                SELECT id, amount, ip, number, region,
                       transaction_date, result, feedback
                FROM transaction_history
                """;
    }

    public record NewTransaction(
            long amount,
            String ip,
            String number,
            String region,
            LocalDateTime date,
            String result,
            String reasons,
            long maxAllowed,
            long maxManual,
            boolean suspiciousIp,
            boolean stolenCard,
            long differentIpCount,
            long differentRegionCount,
            String ruleVersion
    ) {
    }

    public record StoredTransaction(
            long id,
            long amount,
            String ip,
            String number,
            String region,
            LocalDateTime date,
            String result,
            String feedback
    ) {
    }
}
