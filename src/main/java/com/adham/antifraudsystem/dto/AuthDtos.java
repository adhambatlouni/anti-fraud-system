package com.adham.antifraudsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record UserRegistrationRequest(
            @Schema(example = "Demo Merchant")
            @NotBlank(message = "name is required") String name,
            @Schema(example = "merchant")
            @NotBlank(message = "username is required") String username,
            @Schema(example = "ChangeMe-123!")
            @NotBlank(message = "password is required") String password
    ) {
    }

    public record UserResponse(
            Long id,
            String name,
            String username,
            String role
    ) {
    }

    public record UserRoleRequest(
            @Schema(example = "merchant")
            @NotBlank(message = "username is required") String username,
            @Schema(allowableValues = {"MERCHANT", "SUPPORT"}, example = "SUPPORT")
            @NotBlank(message = "role is required")
            @Pattern(
                    regexp = "MERCHANT|SUPPORT",
                    message = "role must be MERCHANT or SUPPORT"
            )
            String role
    ) {
    }

    public record UserAccessRequest(
            @Schema(example = "merchant")
            @NotBlank(message = "username is required") String username,
            @Schema(allowableValues = {"LOCK", "UNLOCK"}, example = "UNLOCK")
            @NotBlank(message = "operation is required")
            @Pattern(
                    regexp = "LOCK|UNLOCK",
                    message = "operation must be LOCK or UNLOCK"
            )
            String operation
    ) {
    }

    public record UserAccessResponse(String status) {
    }

    public record UserDeletionResponse(
            String username,
            String status
    ) {
    }
}
