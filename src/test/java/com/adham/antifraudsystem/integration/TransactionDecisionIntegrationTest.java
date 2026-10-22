package com.adham.antifraudsystem.integration;

import com.adham.antifraudsystem.dto.TransactionDtos.TransactionRequest;
import com.adham.antifraudsystem.dto.TransactionDtos.TransactionResponse;
import com.adham.antifraudsystem.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class TransactionDecisionIntegrationTest {

    private static final String CARD = "4000008449433403";
    private static final String OTHER_CARD = "4000000000000002";
    private static final String PASSWORD = "secure-password";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void resetFraudData() {
        jdbcTemplate.update("DELETE FROM transaction_history");
        jdbcTemplate.update("DELETE FROM suspicious_ips");
        jdbcTemplate.update("DELETE FROM stolen_cards");
        jdbcTemplate.update("DELETE FROM authorities");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update(
                "UPDATE transaction_limits SET max_allowed = 200, max_manual = 1500 WHERE id = 1"
        );

        String password = passwordEncoder.encode(PASSWORD);
        createUser("support", "ROLE_SUPPORT", password);
        createUser("merchant", "ROLE_MERCHANT", password);
    }

    @ParameterizedTest(name = "amount {0} is {1}")
    @CsvSource({
            "200, ALLOWED, none",
            "201, MANUAL_PROCESSING, amount",
            "1500, MANUAL_PROCESSING, amount",
            "1501, PROHIBITED, amount"
    })
    void classifiesAmountBoundariesAndPersistsEveryDecision(
            long amount,
            String expectedResult,
            String expectedInfo) {

        TransactionResponse response = evaluate(
                amount, "192.168.1.1", CARD, "EAP", "2026-10-20T12:00:00"
        );

        assertThat(response.result()).isEqualTo(expectedResult);
        assertThat(response.info()).isEqualTo(expectedInfo);
        assertThat(historyCount()).isOne();
    }

    @Test
    void reportsOnlyProhibitedReasonsWhenBlacklistOverridesManualAmount() {
        jdbcTemplate.update(
                "INSERT INTO suspicious_ips (ip) VALUES (?)",
                "192.168.1.1"
        );
        jdbcTemplate.update(
                "INSERT INTO stolen_cards (number) VALUES (?)",
                CARD
        );

        TransactionResponse response = evaluate(
                300, "192.168.1.1", CARD, "EAP", "2026-10-20T12:00:00"
        );

        assertThat(response.result()).isEqualTo("PROHIBITED");
        assertThat(response.info()).isEqualTo("card-number, ip");
        assertThat(historyCount()).isOne();
    }

    @Test
    void escalatesAtTwoAndThreeDistinctIpsAndRegions() {
        evaluate(100, "192.168.1.1", CARD, "EAP", "2026-10-20T11:00:00");
        evaluate(100, "192.168.1.2", CARD, "ECA", "2026-10-20T11:10:00");

        TransactionResponse manual = evaluate(
                100, "192.168.1.3", CARD, "HIC", "2026-10-20T11:20:00"
        );
        TransactionResponse prohibited = evaluate(
                100, "192.168.1.4", CARD, "LAC", "2026-10-20T11:30:00"
        );

        assertThat(manual.result()).isEqualTo("MANUAL_PROCESSING");
        assertThat(manual.info())
                .isEqualTo("ip-correlation, region-correlation");
        assertThat(prohibited.result()).isEqualTo("PROHIBITED");
        assertThat(prohibited.info())
                .isEqualTo("ip-correlation, region-correlation");
        assertThat(historyCount()).isEqualTo(4);
    }

    @Test
    void includesTheOneHourBoundaryAndIsolatesDifferentCards() {
        evaluate(100, "192.168.1.1", CARD, "EAP", "2026-10-20T10:59:59");
        evaluate(100, "192.168.1.2", CARD, "ECA", "2026-10-20T11:00:00");
        evaluate(100, "192.168.1.3", OTHER_CARD, "HIC", "2026-10-20T11:15:00");
        evaluate(100, "192.168.1.4", CARD, "LAC", "2026-10-20T11:30:00");

        TransactionResponse response = evaluate(
                100, "192.168.1.5", CARD, "MENA", "2026-10-20T12:00:00"
        );

        assertThat(response.result()).isEqualTo("MANUAL_PROCESSING");
        assertThat(response.info())
                .isEqualTo("ip-correlation, region-correlation");
    }

    @Test
    void exposesTheOriginalDecisionEvidenceAfterFraudDataChanges()
            throws Exception {

        jdbcTemplate.update(
                "INSERT INTO suspicious_ips (ip) VALUES (?)",
                "192.168.1.1"
        );

        TransactionResponse response = evaluate(
                300,
                "192.168.1.1",
                CARD,
                "EAP",
                "2026-10-20T12:00:00"
        );
        Long transactionId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM transaction_history",
                Long.class
        );

        jdbcTemplate.update("DELETE FROM suspicious_ips");
        jdbcTemplate.update(
                "UPDATE transaction_limits SET max_allowed = 900, max_manual = 1900 WHERE id = 1"
        );

        assertThat(response.result()).isEqualTo("PROHIBITED");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE transaction_history SET decision_reasons = 'amount' WHERE id = ?",
                transactionId
        )).isInstanceOf(DataAccessException.class);

        mockMvc.perform(get(
                        "/api/v2/antifraud/transactions/{id}/decision",
                        transactionId
                ).with(httpBasic("support", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId")
                        .value(transactionId))
                .andExpect(jsonPath("$.amount").value(300))
                .andExpect(jsonPath("$.result").value("PROHIBITED"))
                .andExpect(jsonPath("$.evidenceAvailable").value(true))
                .andExpect(jsonPath("$.reasons[0]").value("ip"))
                .andExpect(jsonPath("$.appliedLimits.maxAllowed")
                        .value(200))
                .andExpect(jsonPath("$.appliedLimits.maxManual")
                        .value(1500))
                .andExpect(jsonPath("$.signals.suspiciousIp").value(true))
                .andExpect(jsonPath("$.signals.stolenCard").value(false))
                .andExpect(jsonPath("$.signals.differentIpCount").value(0))
                .andExpect(jsonPath("$.signals.differentRegionCount")
                        .value(0))
                .andExpect(jsonPath("$.ruleVersion").value("1.0"))
                .andExpect(jsonPath("$.number").doesNotExist())
                .andExpect(jsonPath("$.ip").doesNotExist());
    }

    @Test
    void identifiesLegacyTransactionsWithoutInventingEvidence()
            throws Exception {

        Long transactionId = insertLegacyTransaction();

        mockMvc.perform(get(
                        "/api/v2/antifraud/transactions/{id}/decision",
                        transactionId
                ).with(httpBasic("support", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidenceAvailable").value(false))
                .andExpect(jsonPath("$.reasons").isEmpty())
                .andExpect(jsonPath("$.appliedLimits").doesNotExist())
                .andExpect(jsonPath("$.signals").doesNotExist())
                .andExpect(jsonPath("$.ruleVersion").doesNotExist());
    }

    @Test
    void protectsDecisionEvidenceAndReturns404ForMissingTransactions()
            throws Exception {

        mockMvc.perform(get(
                        "/api/v2/antifraud/transactions/999999/decision"
                ).with(httpBasic("support", PASSWORD)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(
                        "/api/v2/antifraud/transactions/1/decision"
                ).with(httpBasic("merchant", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    private TransactionResponse evaluate(
            long amount,
            String ip,
            String number,
            String region,
            String date) {

        return transactionService.evaluateTransaction(
                new TransactionRequest(amount, ip, number, region, date)
        );
    }

    private long historyCount() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transaction_history",
                Long.class
        );
        return count == null ? 0 : count;
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

    private Long insertLegacyTransaction() {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO transaction_history
                    (amount, ip, number, region,
                     transaction_date, result, feedback)
                VALUES (100, '192.168.1.1', ?, 'EAP',
                        TIMESTAMP '2026-10-20 12:00:00', 'ALLOWED', '')
                RETURNING id
                """,
                Long.class,
                CARD
        );
    }
}
