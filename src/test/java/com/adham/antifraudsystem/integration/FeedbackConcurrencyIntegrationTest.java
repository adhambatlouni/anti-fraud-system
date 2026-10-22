package com.adham.antifraudsystem.integration;

import com.adham.antifraudsystem.dto.TransactionDtos.FeedbackRequest;
import com.adham.antifraudsystem.dto.TransactionDtos.TransactionHistoryResponse;
import com.adham.antifraudsystem.service.FraudLimitService;
import com.adham.antifraudsystem.service.TransactionFeedbackService;
import com.adham.antifraudsystem.service.TransactionFeedbackService.DuplicateFeedbackException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class FeedbackConcurrencyIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    private TransactionFeedbackService feedbackService;

    @Autowired
    private FraudLimitService fraudLimitService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetFeedbackData() {
        jdbcTemplate.execute("TRUNCATE TABLE audit_events RESTART IDENTITY");
        jdbcTemplate.update("DELETE FROM transaction_history");
        jdbcTemplate.update("DELETE FROM authorities");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update(
                "UPDATE transaction_limits SET max_allowed = 200, max_manual = 1500 WHERE id = 1"
        );
        jdbcTemplate.update(
                "INSERT INTO users (name, username, password, enabled) VALUES (?, ?, ?, ?)",
                "Support",
                "support",
                "unused",
                true
        );
        jdbcTemplate.update(
                "INSERT INTO authorities (username, authority) VALUES (?, ?)",
                "support",
                "ROLE_SUPPORT"
        );
    }

    @Test
    @Timeout(20)
    void appliesFeedbackExactlyOnceWhenRequestsRaceForTheSameTransaction()
            throws Exception {

        long transactionId = insertTransaction(210, "MANUAL_PROCESSING");
        FeedbackRequest request = new FeedbackRequest(transactionId, "ALLOWED");

        List<Object> outcomes = runConcurrently(
                () -> feedbackService.submitFeedback(request, "support"),
                () -> feedbackService.submitFeedback(request, "support")
        );

        assertThat(outcomes.stream()
                .filter(TransactionHistoryResponse.class::isInstance))
                .hasSize(1);
        assertThat(outcomes.stream()
                .filter(DuplicateFeedbackException.class::isInstance))
                .hasSize(1);

        assertThat(storedFeedback(transactionId)).isEqualTo("ALLOWED");
        assertThat(storedResult(transactionId)).isEqualTo("MANUAL_PROCESSING");
        assertThat(fraudLimitService.getCurrentLimits())
                .isEqualTo(new FraudLimitService.Limits(202, 1_500));
        assertThat(auditCount()).isEqualTo(2);
    }

    @Test
    @Timeout(20)
    void serializesDifferentFeedbackRequestsUpdatingTheSharedLimitsRow()
            throws Exception {

        long firstId = insertTransaction(210, "MANUAL_PROCESSING");
        long secondId = insertTransaction(300, "MANUAL_PROCESSING");

        List<Object> outcomes = runConcurrently(
                () -> feedbackService.submitFeedback(
                        new FeedbackRequest(firstId, "ALLOWED"),
                        "support"
                ),
                () -> feedbackService.submitFeedback(
                        new FeedbackRequest(secondId, "ALLOWED"),
                        "support"
                )
        );

        assertThat(outcomes)
                .allMatch(TransactionHistoryResponse.class::isInstance);
        assertThat(storedFeedback(firstId)).isEqualTo("ALLOWED");
        assertThat(storedFeedback(secondId)).isEqualTo("ALLOWED");

        FraudLimitService.Limits limits = fraudLimitService.getCurrentLimits();
        assertThat(limits.maxAllowed()).isIn(218L, 222L);
        assertThat(limits.maxManual()).isEqualTo(1_500);
        assertThat(auditCount()).isEqualTo(4);
    }

    private List<Object> runConcurrently(
            Callable<Object> first,
            Callable<Object> second) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Object> firstResult = executor.submit(
                    awaitStart(first, ready, start)
            );
            Future<Object> secondResult = executor.submit(
                    awaitStart(second, ready, start)
            );

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            return List.of(
                    firstResult.get(10, TimeUnit.SECONDS),
                    secondResult.get(10, TimeUnit.SECONDS)
            );
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private Callable<Object> awaitStart(
            Callable<Object> action,
            CountDownLatch ready,
            CountDownLatch start) {

        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent test did not start");
            }

            try {
                return action.call();
            } catch (RuntimeException exception) {
                return exception;
            }
        };
    }

    private long insertTransaction(long amount, String result) {
        Long id = jdbcTemplate.queryForObject(
                """
                INSERT INTO transaction_history
                    (amount, ip, number, region, transaction_date, result, feedback)
                VALUES (?, ?, ?, ?, ?, ?, '')
                RETURNING id
                """,
                Long.class,
                amount,
                "192.168.1.1",
                "4000008449433403",
                "EAP",
                Timestamp.valueOf(LocalDateTime.of(2026, 10, 20, 12, 0)),
                result
        );
        return id;
    }

    private String storedFeedback(long transactionId) {
        return jdbcTemplate.queryForObject(
                "SELECT feedback FROM transaction_history WHERE id = ?",
                String.class,
                transactionId
        );
    }

    private String storedResult(long transactionId) {
        return jdbcTemplate.queryForObject(
                "SELECT result FROM transaction_history WHERE id = ?",
                String.class,
                transactionId
        );
    }

    private long auditCount() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_events",
                Long.class
        );
        return count == null ? 0 : count;
    }
}
