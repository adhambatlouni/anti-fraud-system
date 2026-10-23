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
@Tag(name = "Fraud blacklists")
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
    @Operation(
            summary = "Flag a suspicious IP",
            description = "Adds a valid IPv4 address to the fraud signal list. Future transactions "
                    + "from this address are prohibited. **Required role: `SUPPORT`.**"
    )
    public SuspiciousIpResponse addIp(
            @Valid @RequestBody SuspiciousIpRequest request,
            @Parameter(hidden = true) Principal principal) {

        return suspiciousIpService.addIp(request, principal.getName());
    }

    @GetMapping("/suspicious-ip")
    @Operation(
            summary = "List suspicious IPs",
            description = "Returns flagged IP addresses in insertion order. **Required role: `SUPPORT`.**"
    )
    public List<SuspiciousIpResponse> getAllIps() {
        return suspiciousIpService.getAllIps();
    }

    @DeleteMapping("/suspicious-ip/{ip}")
    @Operation(
            summary = "Unflag a suspicious IP",
            description = "Removes an IP from the fraud signal list. **Required role: `SUPPORT`.**"
    )
    public DeletionResponse deleteIp(
            @Parameter(description = "IPv4 address to unflag", example = "192.168.1.50")
            @PathVariable String ip,
            @Parameter(hidden = true) Principal principal) {

        return suspiciousIpService.deleteIp(ip, principal.getName());
    }

    @PostMapping("/stolencard")
    @Operation(
            summary = "Flag a stolen card",
            description = "Adds a valid synthetic card number to the stolen-card list. Future "
                    + "transactions using it are prohibited. **Required role: `SUPPORT`.**"
    )
    public StolenCardResponse addCard(
            @Valid @RequestBody StolenCardRequest request,
            @Parameter(hidden = true) Principal principal) {

        return stolenCardService.addCard(request, principal.getName());
    }

    @GetMapping("/stolencard")
    @Operation(
            summary = "List stolen cards",
            description = "Returns flagged synthetic card numbers in insertion order. "
                    + "**Required role: `SUPPORT`.**"
    )
    public List<StolenCardResponse> getAllCards() {
        return stolenCardService.getAllCards();
    }

    @DeleteMapping("/stolencard/{number}")
    @Operation(
            summary = "Unflag a stolen card",
            description = "Removes a synthetic card number from the stolen-card list. "
                    + "**Required role: `SUPPORT`.**"
    )
    public DeletionResponse deleteCard(
            @Parameter(description = "Synthetic card number to unflag", example = "4000008449433403")
            @PathVariable String number,
            @Parameter(hidden = true) Principal principal) {

        return stolenCardService.deleteCard(number, principal.getName());
    }
}
