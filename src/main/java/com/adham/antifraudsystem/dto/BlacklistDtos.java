package com.adham.antifraudsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public final class BlacklistDtos {

    private BlacklistDtos() {
    }

    public record SuspiciousIpRequest(
            @Schema(description = "IPv4 address to flag", example = "192.168.1.50")
            @NotBlank(message = "ip is required") String ip) {
    }

    public record SuspiciousIpResponse(
            @Schema(example = "1") Long id,
            @Schema(example = "192.168.1.50") String ip) {
    }

    public record StolenCardRequest(
            @Schema(
                    description = "Synthetic 16-digit card number",
                    example = "4000008449433403"
            )
            @NotBlank(message = "number is required") String number) {
    }

    public record StolenCardResponse(
            @Schema(example = "1") Long id,
            @Schema(description = "Synthetic card number", example = "4000008449433403") String number) {
    }

    public record DeletionResponse(
            @Schema(example = "IP 192.168.1.50 successfully removed!") String status) {
    }
}
