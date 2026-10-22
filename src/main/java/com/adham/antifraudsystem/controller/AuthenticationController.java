package com.adham.antifraudsystem.controller;

import com.adham.antifraudsystem.config.OpenApiConfig;
import com.adham.antifraudsystem.dto.AuthDtos.UserAccessRequest;
import com.adham.antifraudsystem.dto.AuthDtos.UserAccessResponse;
import com.adham.antifraudsystem.dto.AuthDtos.UserDeletionResponse;
import com.adham.antifraudsystem.dto.AuthDtos.UserRegistrationRequest;
import com.adham.antifraudsystem.dto.AuthDtos.UserResponse;
import com.adham.antifraudsystem.dto.AuthDtos.UserRoleRequest;
import com.adham.antifraudsystem.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Users", description = "Registration and user administration")
public class AuthenticationController {

    private final UserService userService;

    public AuthenticationController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/user")
    @Operation(
            summary = "Register a user",
            description = "Public endpoint. The first user becomes the enabled administrator."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered"),
            @ApiResponse(responseCode = "400", description = "Invalid registration request"),
            @ApiResponse(responseCode = "409", description = "Username already exists")
    })
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody UserRegistrationRequest request) {
        UserResponse user = userService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(user);
    }

    @DeleteMapping("/user/{username}")
    @Operation(
            summary = "Delete a user",
            description = "Required role: ADMINISTRATOR",
            security = @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH)
    )
    public UserDeletionResponse deleteUser(
            @PathVariable String username) {
        return userService.deleteUser(username);
    }

    @GetMapping("/list")
    @Operation(
            summary = "List users",
            description = "Required role: ADMINISTRATOR or SUPPORT",
            security = @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH)
    )
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/access")
    @Operation(
            summary = "Lock or unlock a user",
            description = "Required role: ADMINISTRATOR",
            security = @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH)
    )
    public UserAccessResponse changeUserAccess(
            @Valid @RequestBody UserAccessRequest request) {
        return userService.changeUserAccess(request);
    }

    @PutMapping("/role")
    @Operation(
            summary = "Change a user's role",
            description = "Required role: ADMINISTRATOR",
            security = @SecurityRequirement(name = OpenApiConfig.BASIC_AUTH)
    )
    public UserResponse changeUserRole(
            @Valid @RequestBody UserRoleRequest request) {
        return userService.changeUserRole(request);
    }
}
