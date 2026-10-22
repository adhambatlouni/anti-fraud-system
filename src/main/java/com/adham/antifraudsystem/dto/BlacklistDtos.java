package com.adham.antifraudsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public final class BlacklistDtos {

    private BlacklistDtos() {
    }

    public record SuspiciousIpRequest(
            @Schema(example = "192.168.1.1")
            @NotBlank(message = "ip is required") String ip) {
    }

    public record SuspiciousIpResponse(Long id, String ip) {
    }

    public record StolenCardRequest(
            @Schema(
                    description = "Synthetic 16-digit card number",
                    example = "4000008449433403"
            )
            @NotBlank(message = "number is required") String number) {
    }

    public record StolenCardResponse(Long id, String number) {
    }

    public record DeletionResponse(String status) {
    }
}
