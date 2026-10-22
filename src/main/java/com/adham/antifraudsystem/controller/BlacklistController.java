package com.adham.antifraudsystem.controller;

import com.adham.antifraudsystem.config.OpenApiConfig;
import com.adham.antifraudsystem.dto.BlacklistDtos.DeletionResponse;
import com.adham.antifraudsystem.dto.BlacklistDtos.StolenCardRequest;
import com.adham.antifraudsystem.dto.BlacklistDtos.StolenCardResponse;
import com.adham.antifraudsystem.dto.BlacklistDtos.SuspiciousIpRequest;
import com.adham.antifraudsystem.dto.BlacklistDtos.SuspiciousIpResponse;
import com.adham.antifraudsystem.service.StolenCardService;
import com.adham.antifraudsystem.service.SuspiciousIpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/antifraud")
@Tag(name = "Fraud blacklists", description = "Suspicious IP and stolen-card management")
@SecurityRequirement(name = OpenApiConfig.BASIC_AUTH)
public class BlacklistController {

    private final SuspiciousIpService suspiciousIpService;
    private final StolenCardService stolenCardService;

    public BlacklistController(
            SuspiciousIpService suspiciousIpService,
            StolenCardService stolenCardService) {

        this.suspiciousIpService = suspiciousIpService;
        this.stolenCardService = stolenCardService;
    }

    @PostMapping("/suspicious-ip")
    @Operation(summary = "Add a suspicious IP", description = "Required role: SUPPORT")
    public SuspiciousIpResponse addIp(
            @Valid @RequestBody SuspiciousIpRequest request,
            @Parameter(hidden = true) Principal principal) {

        return suspiciousIpService.addIp(request, principal.getName());
    }

    @GetMapping("/suspicious-ip")
    @Operation(summary = "List suspicious IPs", description = "Required role: SUPPORT")
    public List<SuspiciousIpResponse> getAllIps() {
        return suspiciousIpService.getAllIps();
    }

    @DeleteMapping("/suspicious-ip/{ip}")
    @Operation(summary = "Remove a suspicious IP", description = "Required role: SUPPORT")
    public DeletionResponse deleteIp(
            @PathVariable String ip,
            @Parameter(hidden = true) Principal principal) {

        return suspiciousIpService.deleteIp(ip, principal.getName());
    }

    @PostMapping("/stolencard")
    @Operation(summary = "Add a stolen card", description = "Required role: SUPPORT")
    public StolenCardResponse addCard(
            @Valid @RequestBody StolenCardRequest request,
            @Parameter(hidden = true) Principal principal) {

        return stolenCardService.addCard(request, principal.getName());
    }

    @GetMapping("/stolencard")
    @Operation(summary = "List stolen cards", description = "Required role: SUPPORT")
    public List<StolenCardResponse> getAllCards() {
        return stolenCardService.getAllCards();
    }

    @DeleteMapping("/stolencard/{number}")
    @Operation(summary = "Remove a stolen card", description = "Required role: SUPPORT")
    public DeletionResponse deleteCard(
            @PathVariable String number,
            @Parameter(hidden = true) Principal principal) {

        return stolenCardService.deleteCard(number, principal.getName());
    }
}
