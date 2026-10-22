package com.adham.antifraudsystem.controller;

import com.adham.antifraudsystem.config.OpenApiConfig;
import com.adham.antifraudsystem.dto.TransactionDtos.OperationalMetricsResponse;
import com.adham.antifraudsystem.service.OperationalMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/antifraud/metrics")
@Tag(
        name = "Operational metrics",
        description = "Aggregated fraud workflow reporting"
)
@SecurityRequirement(name = OpenApiConfig.BASIC_AUTH)
public class OperationalMetricsController {

    private final OperationalMetricsService operationalMetricsService;

    public OperationalMetricsController(
            OperationalMetricsService operationalMetricsService) {
        this.operationalMetricsService = operationalMetricsService;
    }

    @GetMapping
    @Operation(
            summary = "Get fraud operations metrics",
            description = "Required role: ADMINISTRATOR or SUPPORT. "
                    + "The reporting window is inclusive and limited to 366 days."
    )
    public OperationalMetricsResponse getMetrics(
            @Parameter(example = "2026-10-01T00:00:00")
            @RequestParam("from") String from,
            @Parameter(example = "2026-10-31T23:59:59")
            @RequestParam("to") String to) {

        return operationalMetricsService.getMetrics(from, to);
    }
}
