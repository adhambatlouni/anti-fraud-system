package com.adham.antifraudsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public final class AuditDtos {

    private AuditDtos() {
    }

    @Schema(description = "Append-only record of a sensitive fraud operation")
    public record AuditEventResponse(
            @Schema(example = "2") long id,
            @Schema(example = "3") long actorId,
            @Schema(example = "support") String actorUsername,
            @Schema(example = "LIMIT_UPDATED") String action,
            @Schema(example = "TRANSACTION_LIMITS") String resourceType,
            @Schema(example = "1") String resourceReference,
            @Schema(example = "maxAllowed=200,maxManual=1500") String beforeValue,
            @Schema(example = "maxAllowed=320,maxManual=1500") String afterValue,
            @Schema(example = "2026-10-23T14:36:32.838993Z") Instant createdAt
    ) {
    }
}
