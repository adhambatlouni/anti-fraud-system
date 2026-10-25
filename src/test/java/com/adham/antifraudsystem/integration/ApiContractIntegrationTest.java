package com.adham.antifraudsystem.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ApiContractIntegrationTest {

    private static final String PASSWORD = "secure-password";
    private static final String VALID_TRANSACTION = """
            {
              "amount": 100,
              "ip": "192.168.1.1",
              "number": "4000008449433403",
              "region": "EAP",
              "date": "2026-10-20T12:00:00"
            }
            """;

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
    void resetDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE audit_events RESTART IDENTITY");
        jdbcTemplate.update("DELETE FROM transaction_history");
        jdbcTemplate.update("DELETE FROM suspicious_ips");
        jdbcTemplate.update("DELETE FROM stolen_cards");
        jdbcTemplate.update("DELETE FROM authorities");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update(
                "UPDATE transaction_limits SET max_allowed = 200, max_manual = 1500 WHERE id = 1"
        );

        String encodedPassword = passwordEncoder.encode(PASSWORD);
        createUser("admin", "ROLE_ADMINISTRATOR", true, encodedPassword);
        createUser("merchant", "ROLE_MERCHANT", true, encodedPassword);
        createUser("locked", "ROLE_MERCHANT", false, encodedPassword);
        createUser("support", "ROLE_SUPPORT", true, encodedPassword);
    }

    @Test
    void rejectsMissingAuthenticationWith401() throws Exception {
        mockMvc.perform(post("/api/antifraud/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TRANSACTION))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exposesOnlyTheActuatorHealthEndpointWithoutAuthentication()
            throws Exception {

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exposesOpenApiDocumentationWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title")
                        .value("Anti-Fraud System API"))
                .andExpect(jsonPath(
                        "$['components']['securitySchemes']['basicAuth']['scheme']"
                ).value("basic"))
                .andExpect(jsonPath(
                        "$['paths']['/api/antifraud/transaction']['put']['responses']['422']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/auth/user']['post']['responses']['201']"
                ).exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/v2/antifraud/transactions/{transactionId}/decision']"
                                + "['get']['operationId']"
                ).value("inspectOriginalFraudDecision"))
                .andExpect(jsonPath(
                        "$['paths']['/api/v2/antifraud/transactions/{transactionId}/decision']"
                                + "['get']['responses']['200']['content']"
                                + "['application/json']['examples']"
                                + "['Amount-based manual review']['value']"
                                + "['reasons'][0]"
                ).value("amount"))
                .andExpect(jsonPath(
                        "$['paths']['/api/v2/antifraud/transactions/{transactionId}/decision']"
                                + "['get']['responses']['200']['description']"
                ).value("Response body"));

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/swagger-ui/swagger-ui.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "#operations-Investigation-inspectOriginalFraudDecision"
                )))
                .andExpect(content().string(containsString(
                        "#operations-Investigation-search"
                )))
                .andExpect(content().string(containsString(
                        "font-size: 15px"
                )));
    }

    @Test
    void rejectsLockedAccountWith401() throws Exception {
        mockMvc.perform(post("/api/antifraud/transaction")
                        .with(httpBasic("locked", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TRANSACTION))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWrongRoleWith403() throws Exception {
        mockMvc.perform(get("/api/antifraud/history")
                        .with(httpBasic("merchant", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsProblemDetailForInvalidTransaction() throws Exception {
        String invalidTransaction = VALID_TRANSACTION.replace(
                "\"amount\": 100",
                "\"amount\": 0"
        );

        mockMvc.perform(post("/api/antifraud/transaction")
                        .with(httpBasic("merchant", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidTransaction))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.errors.amount")
                        .value("amount must be positive"));
    }

    @Test
    void returnsProblemDetailForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/antifraud/transaction")
                        .with(httpBasic("merchant", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail")
                        .value("The request body is missing or malformed"));
    }

    @Test
    void returns404WhenBlacklistEntryDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/antifraud/suspicious-ip/192.168.1.1")
                        .with(httpBasic("support", PASSWORD)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void returns409ForDuplicateBlacklistEntry() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO suspicious_ips (ip) VALUES (?)",
                "192.168.1.1"
        );

        mockMvc.perform(post("/api/antifraud/suspicious-ip")
                        .with(httpBasic("support", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ip\":\"192.168.1.1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void recordsAndExposesRedactedCardAuditEvents() throws Exception {
        String card = "4000008449433403";

        mockMvc.perform(post("/api/antifraud/stolencard")
                        .with(httpBasic("support", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number\":\"" + card + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/antifraud/stolencard/{number}", card)
                        .with(httpBasic("support", PASSWORD)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/antifraud/audit")
                        .param("limit", "10")
                        .with(httpBasic("support", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actorUsername").value("support"))
                .andExpect(jsonPath("$[0].action").value("CARD_UNFLAGGED"))
                .andExpect(jsonPath("$[0].resourceReference")
                        .value("************3403"))
                .andExpect(jsonPath("$[1].action").value("CARD_FLAGGED"))
                .andExpect(jsonPath("$[1].resourceReference")
                        .value("************3403"));
    }

    @Test
    void recordsFeedbackAndLimitUpdatesTogether() throws Exception {
        Long transactionId = insertTransaction("MANUAL_PROCESSING");

        mockMvc.perform(put("/api/antifraud/transaction")
                        .with(httpBasic("support", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionId": %d,
                                  "feedback": "ALLOWED"
                                }
                                """.formatted(transactionId)))
                .andExpect(status().isOk());

        List<String> actions = jdbcTemplate.queryForList(
                "SELECT action FROM audit_events ORDER BY id",
                String.class
        );
        List<String> actors = jdbcTemplate.queryForList(
                "SELECT actor_username FROM audit_events ORDER BY id",
                String.class
        );

        assertThat(actions).containsExactly(
                "FEEDBACK_SUBMITTED",
                "LIMIT_UPDATED"
        );
        assertThat(actors).containsOnly("support");
    }

    @Test
    void rejectsAuditPageLimitsOutsideTheAllowedRange() throws Exception {
        mockMvc.perform(get("/api/antifraud/audit")
                        .param("limit", "101")
                        .with(httpBasic("support", PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void returns422WhenFeedbackMatchesOriginalResult() throws Exception {
        Long transactionId = insertTransaction("ALLOWED");

        mockMvc.perform(put("/api/antifraud/transaction")
                        .with(httpBasic("support", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionId": %d,
                                  "feedback": "ALLOWED"
                                }
                                """.formatted(transactionId)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void exposesOnlyTheCanonicalAccessRoute() throws Exception {
        String request = """
                {"username":"merchant","operation":"LOCK"}
                """;

        mockMvc.perform(put("/api/auth/access")
                        .with(httpBasic("admin", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/auth/access/")
                        .with(httpBasic("admin", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound());
    }

    private void createUser(
            String username,
            String authority,
            boolean enabled,
            String encodedPassword) {

        jdbcTemplate.update(
                "INSERT INTO users (name, username, password, enabled) VALUES (?, ?, ?, ?)",
                username,
                username,
                encodedPassword,
                enabled
        );
        jdbcTemplate.update(
                "INSERT INTO authorities (username, authority) VALUES (?, ?)",
                username,
                authority
        );
    }

    private Long insertTransaction(String result) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO transaction_history
                    (amount, ip, number, region, transaction_date, result, feedback)
                VALUES (?, ?, ?, ?, ?, ?, '')
                RETURNING id
                """,
                Long.class,
                100,
                "192.168.1.1",
                "4000008449433403",
                "EAP",
                Timestamp.valueOf(LocalDateTime.of(2026, 10, 20, 12, 0)),
                result
        );
    }
}
