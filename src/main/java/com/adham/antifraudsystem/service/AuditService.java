package com.adham.antifraudsystem.service;

import com.adham.antifraudsystem.dto.AuditDtos.AuditEventResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AuditService {

    private final JdbcTemplate jdbcTemplate;

    public AuditService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            String actorUsername,
            Action action,
            ResourceType resourceType,
            String resourceReference,
            String beforeValue,
            String afterValue) {

        Actor actor = jdbcTemplate.queryForObject(
                "SELECT id, username FROM users WHERE username = ?",
                (rs, rowNum) -> new Actor(
                        rs.getLong("id"),
                        rs.getString("username")
                ),
                actorUsername
        );

        jdbcTemplate.update(
                """
                INSERT INTO audit_events
                    (actor_id, actor_username, action, resource_type,
                     resource_reference, before_value, after_value)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                actor.id(),
                actor.username(),
                action.name(),
                resourceType.name(),
                resourceReference,
                beforeValue,
                afterValue
        );
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> getRecent(int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException(
                    "Audit limit must be between 1 and 100"
            );
        }

        return jdbcTemplate.query(
                """
                SELECT id, actor_id, actor_username, action, resource_type,
                       resource_reference, before_value, after_value, created_at
                FROM audit_events
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new AuditEventResponse(
                        rs.getLong("id"),
                        rs.getLong("actor_id"),
                        rs.getString("actor_username"),
                        rs.getString("action"),
                        rs.getString("resource_type"),
                        rs.getString("resource_reference"),
                        rs.getString("before_value"),
                        rs.getString("after_value"),
                        rs.getObject("created_at", OffsetDateTime.class)
                                .toInstant()
                ),
                limit
        );
    }

    public enum Action {
        BLACKLIST_IP_ADDED,
        BLACKLIST_IP_REMOVED,
        CARD_FLAGGED,
        CARD_UNFLAGGED,
        FEEDBACK_SUBMITTED,
        LIMIT_UPDATED
    }

    public enum ResourceType {
        SUSPICIOUS_IP,
        STOLEN_CARD,
        TRANSACTION,
        TRANSACTION_LIMITS
    }

    private record Actor(long id, String username) {
    }
}
