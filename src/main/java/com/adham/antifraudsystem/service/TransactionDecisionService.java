package com.adham.antifraudsystem.service;

import com.adham.antifraudsystem.dto.TransactionDtos.DecisionLimits;
import com.adham.antifraudsystem.dto.TransactionDtos.DecisionSignals;
import com.adham.antifraudsystem.dto.TransactionDtos.TransactionDecisionResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class TransactionDecisionService {

    private final JdbcTemplate jdbcTemplate;

    public TransactionDecisionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public TransactionDecisionResponse getDecision(long transactionId) {
        if (transactionId < 1) {
            throw new IllegalArgumentException(
                    "transactionId must be positive"
            );
        }

        return jdbcTemplate.queryForObject(
                """
                SELECT id, amount, result, decision_reasons,
                       applied_max_allowed, applied_max_manual,
                       suspicious_ip_at_decision, stolen_card_at_decision,
                       different_ip_count, different_region_count, rule_version
                FROM transaction_history
                WHERE id = ?
                """,
                (rs, rowNum) -> {
                    String ruleVersion = rs.getString("rule_version");
                    boolean evidenceAvailable = ruleVersion != null;

                    return new TransactionDecisionResponse(
                            rs.getLong("id"),
                            rs.getLong("amount"),
                            rs.getString("result"),
                            evidenceAvailable,
                            reasons(rs.getString("decision_reasons")),
                            evidenceAvailable
                                    ? new DecisionLimits(
                                    rs.getLong("applied_max_allowed"),
                                    rs.getLong("applied_max_manual")
                            )
                                    : null,
                            evidenceAvailable
                                    ? new DecisionSignals(
                                    rs.getBoolean(
                                            "suspicious_ip_at_decision"
                                    ),
                                    rs.getBoolean(
                                            "stolen_card_at_decision"
                                    ),
                                    rs.getLong("different_ip_count"),
                                    rs.getLong("different_region_count")
                            )
                                    : null,
                            ruleVersion
                    );
                },
                transactionId
        );
    }

    private List<String> reasons(String value) {
        if (value == null || value.equals("none")) {
            return List.of();
        }

        return Arrays.stream(value.split(", ")).toList();
    }
}
