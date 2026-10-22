package com.adham.antifraudsystem.integration;

import com.adham.antifraudsystem.service.FraudLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class DatabaseMigrationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer("postgres:18");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FraudLimitService fraudLimitService;

    @Test
    void flywayCreatesSchemaWithDefaultFraudLimits() {
        List<String> appliedVersions = jdbcTemplate.queryForList(
                """
                SELECT version
                FROM flyway_schema_history
                WHERE success = TRUE AND version IS NOT NULL
                ORDER BY installed_rank
                """,
                String.class
        );

        FraudLimitService.Limits limits = fraudLimitService.getCurrentLimits();

        assertThat(appliedVersions)
                .containsExactly("1", "2", "3", "4", "5");
        assertThat(limits.maxAllowed()).isEqualTo(200);
        assertThat(limits.maxManual()).isEqualTo(1500);
    }

    @Test
    void createsIndexesForCorrelationAndCardHistoryQueries() {
        List<String> indexNames = jdbcTemplate.queryForList(
                """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = current_schema()
                  AND tablename = 'transaction_history'
                """,
                String.class
        );

        assertThat(indexNames).contains(
                "transaction_history_card_time_idx",
                "transaction_history_card_id_idx",
                "transaction_history_date_id_idx",
                "transaction_history_result_date_id_idx",
                "transaction_history_region_date_id_idx",
                "transaction_history_feedback_date_id_idx",
                "transaction_history_card_last4_date_id_idx"
        );
    }

    @Test
    void createsAppendOnlyAuditStorage() {
        Integer auditTableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = current_schema()
                  AND table_name = 'audit_events'
                """,
                Integer.class
        );

        Integer appendOnlyTriggerCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM pg_trigger
                WHERE tgname = 'audit_events_append_only'
                  AND NOT tgisinternal
                """,
                Integer.class
        );

        assertThat(auditTableCount).isOne();
        assertThat(appendOnlyTriggerCount).isOne();
    }

    @Test
    void addsImmutableDecisionSnapshotColumns() {
        List<String> columnNames = jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = 'transaction_history'
                """,
                String.class
        );

        assertThat(columnNames).contains(
                "decision_reasons",
                "applied_max_allowed",
                "applied_max_manual",
                "suspicious_ip_at_decision",
                "stolen_card_at_decision",
                "different_ip_count",
                "different_region_count",
                "rule_version"
        );

        Integer immutableTriggerCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM pg_trigger
                WHERE tgname = 'transaction_decision_snapshot_immutable'
                  AND NOT tgisinternal
                """,
                Integer.class
        );

        assertThat(immutableTriggerCount).isOne();
    }

}
