package com.adham.antifraudsystem.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class TransactionSearchIntegrationTest {

    private static final String PASSWORD = "secure-password";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM transaction_history");
        jdbcTemplate.update("DELETE FROM authorities");
        jdbcTemplate.update("DELETE FROM users");

        String password = passwordEncoder.encode(PASSWORD);
        createUser("support", "ROLE_SUPPORT", password);
        createUser("merchant", "ROLE_MERCHANT", password);
        createUser("admin", "ROLE_ADMINISTRATOR", password);

        insertTransaction(100, "192.168.1.1", "4000008449433403",
                "EAP", 10, "ALLOWED", "");
        insertTransaction(300, "192.168.1.2", "4000008449433403",
                "EAP", 11, "MANUAL_PROCESSING", "ALLOWED");
        insertTransaction(1800, "192.168.1.3", "4000000000000002",
                "ECA", 12, "PROHIBITED", "");
        insertTransaction(400, "192.168.1.4", "4000008449433403",
                "EAP", 13, "MANUAL_PROCESSING", "");
    }

    @Test
    void filtersTransactionsAndMasksCardReferences() throws Exception {
        mockMvc.perform(get("/api/v2/antifraud/transactions")
                        .param("result", "MANUAL_PROCESSING")
                        .param("region", "EAP")
                        .param("feedbackStatus", "REVIEWED")
                        .param("cardLast4", "3403")
                        .param("from", "2026-10-20T10:30:00")
                        .param("to", "2026-10-20T11:30:00")
                        .with(httpBasic("support", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].amount").value(300))
                .andExpect(jsonPath("$.content[0].cardReference")
                        .value("************3403"))
                .andExpect(jsonPath("$.content[0].number").doesNotExist());
    }

    @Test
    void paginatesWithDeterministicAllowedSorting() throws Exception {
        mockMvc.perform(get("/api/v2/antifraud/transactions")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "amount")
                        .param("direction", "asc")
                        .with(httpBasic("support", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].amount").value(400))
                .andExpect(jsonPath("$.content[1].amount").value(1800));
    }

    @Test
    void rejectsInvalidSearchParameters() throws Exception {
        mockMvc.perform(get("/api/v2/antifraud/transactions")
                        .param("sort", "number")
                        .with(httpBasic("support", PASSWORD)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v2/antifraud/transactions")
                        .param("from", "2026-10-21T12:00:00")
                        .param("to", "2026-10-20T12:00:00")
                        .with(httpBasic("support", PASSWORD)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v2/antifraud/transactions")
                        .param("size", "101")
                        .with(httpBasic("support", PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void permitsOnlySupportUsers() throws Exception {
        mockMvc.perform(get("/api/v2/antifraud/transactions")
                        .with(httpBasic("merchant", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void reportsBoundedOperationalMetricsWithoutSensitiveData()
            throws Exception {

        mockMvc.perform(get("/api/v2/antifraud/metrics")
                        .param("from", "2026-10-20T09:00:00")
                        .param("to", "2026-10-20T14:00:00")
                        .with(httpBasic("support", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisions.total").value(4))
                .andExpect(jsonPath("$.decisions.allowed").value(1))
                .andExpect(jsonPath("$.decisions.manualProcessing").value(2))
                .andExpect(jsonPath("$.decisions.prohibited").value(1))
                .andExpect(jsonPath("$.reviews.pending").value(3))
                .andExpect(jsonPath("$.reviews.reviewed").value(1))
                .andExpect(jsonPath("$.regions[0].region").value("EAP"))
                .andExpect(jsonPath("$.regions[0].count").value(3))
                .andExpect(jsonPath("$.regions[1].region").value("ECA"))
                .andExpect(jsonPath("$.currentLimits.maxAllowed").value(200))
                .andExpect(jsonPath("$.currentLimits.maxManual").value(1500))
                .andExpect(jsonPath("$.number").doesNotExist())
                .andExpect(jsonPath("$.cardReference").doesNotExist());
    }

    @Test
    void allowsAdministratorsToReadMetrics() throws Exception {
        mockMvc.perform(get("/api/v2/antifraud/metrics")
                        .param("from", "2026-10-20T09:00:00")
                        .param("to", "2026-10-20T14:00:00")
                        .with(httpBasic("admin", PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidOrUnboundedMetricsRequests() throws Exception {
        mockMvc.perform(get("/api/v2/antifraud/metrics")
                        .param("from", "2026-10-21T00:00:00")
                        .param("to", "2026-10-20T00:00:00")
                        .with(httpBasic("support", PASSWORD)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v2/antifraud/metrics")
                        .param("from", "2025-01-01T00:00:00")
                        .param("to", "2026-10-20T00:00:00")
                        .with(httpBasic("support", PASSWORD)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v2/antifraud/metrics")
                        .param("from", "not-a-date")
                        .param("to", "2026-10-20T00:00:00")
                        .with(httpBasic("support", PASSWORD)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v2/antifraud/metrics")
                        .param("from", "2026-10-20T00:00:00")
                        .param("to", "2026-10-21T00:00:00")
                        .with(httpBasic("merchant", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    private void createUser(
            String username,
            String authority,
            String password) {

        jdbcTemplate.update(
                "INSERT INTO users (name, username, password, enabled) VALUES (?, ?, ?, TRUE)",
                username,
                username,
                password
        );
        jdbcTemplate.update(
                "INSERT INTO authorities (username, authority) VALUES (?, ?)",
                username,
                authority
        );
    }

    private void insertTransaction(
            long amount,
            String ip,
            String number,
            String region,
            int hour,
            String result,
            String feedback) {

        jdbcTemplate.update(
                """
                INSERT INTO transaction_history
                    (amount, ip, number, region, transaction_date, result, feedback)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                amount,
                ip,
                number,
                region,
                Timestamp.valueOf(LocalDateTime.of(2026, 10, 20, hour, 0)),
                result,
                feedback
        );
    }
}
