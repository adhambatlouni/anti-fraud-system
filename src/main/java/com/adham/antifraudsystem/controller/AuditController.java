package com.adham.antifraudsystem.controller;

import com.adham.antifraudsystem.config.OpenApiConfig;
import com.adham.antifraudsystem.dto.AuditDtos.AuditEventResponse;
import com.adham.antifraudsystem.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "Audit trail")
@SecurityRequirement(name = OpenApiConfig.BASIC_AUTH)
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(
            summary = "List recent audit events",
            description = "Returns append-only records for blacklist changes, feedback, and adaptive "
                    + "limit updates. Card references are masked and newest events appear first. "
                    + "**Required role: `ADMINISTRATOR` or `SUPPORT`.**"
    )
    public List<AuditEventResponse> getRecent(
            @Parameter(description = "Maximum events to return, from 1 to 100", example = "20")
            @RequestParam(defaultValue = "50")
            @Min(1) @Max(100) int limit) {

        return auditService.getRecent(limit);
    }
}
