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

    @Schema(description = "Synthetic transaction submitted for fraud evaluation")
    public record TransactionRequest(
            @Schema(description = "Transaction amount in whole units", example = "800")
            @NotNull(message = "amount is required")
            @Positive(message = "amount must be positive")
            Long amount,
            @Schema(description = "Source IPv4 address", example = "192.168.1.10")
            @NotBlank(message = "ip is required") String ip,
            @Schema(
                    description = "Synthetic 16-digit card number",
                    example = "4000008449433403"
            )
            @NotBlank(message = "number is required") String number,
            @Schema(
                    description = "Transaction region code",
                    allowableValues = {"EAP", "ECA", "HIC", "LAC", "MENA", "SA", "SSA"},
                    example = "EAP"
            )
            @NotBlank(message = "region is required") String region,
            @Schema(
                    description = "Local transaction time using yyyy-MM-dd'T'HH:mm:ss",
                    example = "2026-10-20T12:00:00"
            )
            @NotBlank(message = "date is required") String date
    ) {
    }

    @Schema(description = "Fraud decision and the rules responsible for it")
    public record TransactionResponse(
            @Schema(
                    allowableValues = {"ALLOWED", "MANUAL_PROCESSING", "PROHIBITED"},
                    example = "MANUAL_PROCESSING"
            )
            String result,
            @Schema(
                    description = "Alphabetically ordered reasons, or none",
                    example = "amount, ip-correlation"
            )
            String info
    ) {
    }

    @Schema(description = "Stored transaction with its original decision and later specialist feedback")
    public record TransactionHistoryResponse(
            @Schema(example = "2") Long transactionId,
            @Schema(example = "800") Long amount,
            @Schema(example = "192.168.1.10") String ip,
            @Schema(description = "Synthetic card number", example = "4000008449433403") String number,
            @Schema(example = "EAP") String region,
            @Schema(example = "2026-10-20T12:00:00") String date,
            @Schema(example = "MANUAL_PROCESSING") String result,
            @Schema(description = "Empty until reviewed", example = "ALLOWED") String feedback
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

    @Schema(description = "One bounded page of privacy-safe transaction results")
    public record TransactionSearchPage(
            List<TransactionSearchItem> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }

    @Schema(description = "Aggregated fraud operations report for an inclusive time window")
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

    @Schema(description = "Immutable fraud-rule evidence captured during the original evaluation")
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
