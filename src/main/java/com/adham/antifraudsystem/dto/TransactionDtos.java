package com.adham.antifraudsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.List;

public final class TransactionDtos {

    private TransactionDtos() {
    }

    public record TransactionRequest(
            @Schema(example = "800")
            @NotNull(message = "amount is required")
            @Positive(message = "amount must be positive")
            Long amount,
            @Schema(example = "192.168.1.1")
            @NotBlank(message = "ip is required") String ip,
            @Schema(
                    description = "Synthetic 16-digit card number",
                    example = "4000008449433403"
            )
            @NotBlank(message = "number is required") String number,
            @Schema(example = "EAP")
            @NotBlank(message = "region is required") String region,
            @Schema(example = "2026-10-20T12:00:00")
            @NotBlank(message = "date is required") String date
    ) {
    }

    public record TransactionResponse(
            String result,
            String info
    ) {
    }

    public record TransactionHistoryResponse(
            Long transactionId,
            Long amount,
            String ip,
            String number,
            String region,
            String date,
            String result,
            String feedback
    ) {
    }

    public record FeedbackRequest(
            @Schema(example = "1")
            @NotNull(message = "transactionId is required")
            @Positive(message = "transactionId must be positive")
            Long transactionId,
            @Schema(
                    allowableValues = {"ALLOWED", "MANUAL_PROCESSING", "PROHIBITED"},
                    example = "ALLOWED"
            )
            @NotBlank(message = "feedback is required")
            @Pattern(
                    regexp = "ALLOWED|MANUAL_PROCESSING|PROHIBITED",
                    message = "feedback has an unsupported value"
            )
            String feedback
    ) {
    }

    public record TransactionSearchItem(
            Long transactionId,
            Long amount,
            String ip,
            @Schema(example = "************3403")
            String cardReference,
            String region,
            String date,
            String result,
            String feedback
    ) {
    }

    public record TransactionSearchPage(
            List<TransactionSearchItem> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }

    public record OperationalMetricsResponse(
            String from,
            String to,
            DecisionMetrics decisions,
            ReviewMetrics reviews,
            List<RegionMetric> regions,
            FraudLimitsMetric currentLimits
    ) {
    }

    public record DecisionMetrics(
            long total,
            long allowed,
            long manualProcessing,
            long prohibited
    ) {
    }

    public record ReviewMetrics(
            long pending,
            long reviewed
    ) {
    }

    public record RegionMetric(
            String region,
            long count
    ) {
    }

    public record FraudLimitsMetric(
            long maxAllowed,
            long maxManual
    ) {
    }

    public record TransactionDecisionResponse(
            long transactionId,
            long amount,
            String result,
            boolean evidenceAvailable,
            List<String> reasons,
            DecisionLimits appliedLimits,
            DecisionSignals signals,
            String ruleVersion
    ) {
    }

    public record DecisionLimits(
            long maxAllowed,
            long maxManual
    ) {
    }

    public record DecisionSignals(
            boolean suspiciousIp,
            boolean stolenCard,
            long differentIpCount,
            long differentRegionCount
    ) {
    }
}
