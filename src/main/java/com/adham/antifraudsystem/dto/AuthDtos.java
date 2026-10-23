package com.adham.antifraudsystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class AuthDtos {

    private AuthDtos() {
    }

    @Schema(description = "Credentials and display name for a new account")
    public record UserRegistrationRequest(
            @Schema(description = "Display name", example = "Demo Merchant")
            @NotBlank(message = "name is required") String name,
            @Schema(description = "Case-insensitive login name", example = "merchant")
            @NotBlank(message = "username is required") String username,
            @Schema(description = "Account password", example = "ChangeMe-123!", format = "password")
            @NotBlank(message = "password is required") String password
    ) {
    }

    @Schema(description = "Public account details; passwords are never returned")
    public record UserResponse(
            @Schema(example = "2") Long id,
            @Schema(example = "Demo Merchant") String name,
            @Schema(example = "merchant") String username,
            @Schema(
                    allowableValues = {"ADMINISTRATOR", "MERCHANT", "SUPPORT"},
                    example = "MERCHANT"
            )
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

    public record UserAccessResponse(
            @Schema(example = "User merchant unlocked!") String status) {
    }

    public record UserDeletionResponse(
            @Schema(example = "merchant") String username,
            @Schema(example = "Deleted successfully!") String status
    ) {
    }
}
