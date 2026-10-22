package com.adham.antifraudsystem.dto;

import java.time.Instant;

public final class AuditDtos {

    private AuditDtos() {
    }

    public record AuditEventResponse(
            long id,
            long actorId,
            String actorUsername,
            String action,
            String resourceType,
            String resourceReference,
            String beforeValue,
            String afterValue,
            Instant createdAt
    ) {
    }
}
