package com.adham.antifraudsystem.controller;

import com.adham.antifraudsystem.config.OpenApiConfig;
import com.adham.antifraudsystem.dto.AuditDtos.AuditEventResponse;
import com.adham.antifraudsystem.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/antifraud/audit")
@Tag(name = "Audit", description = "Append-only fraud operations history")
@SecurityRequirement(name = OpenApiConfig.BASIC_AUTH)
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(
            summary = "List recent audit events",
            description = "Required role: ADMINISTRATOR or SUPPORT. Newest events are returned first."
    )
    public List<AuditEventResponse> getRecent(
            @RequestParam(defaultValue = "50")
            @Min(1) @Max(100) int limit) {

        return auditService.getRecent(limit);
    }
}
