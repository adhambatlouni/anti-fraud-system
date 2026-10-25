package com.adham.antifraudsystem.controller;

import com.adham.antifraudsystem.config.OpenApiConfig;
import com.adham.antifraudsystem.dto.TransactionDtos.TransactionSearchPage;
import com.adham.antifraudsystem.dto.TransactionDtos.TransactionDecisionResponse;
import com.adham.antifraudsystem.service.TransactionDecisionService;
import com.adham.antifraudsystem.service.TransactionSearchService;
import com.adham.antifraudsystem.service.TransactionSearchService.SearchCriteria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/antifraud/transactions")
@Tag(
        name = "Investigation"
)
@SecurityRequirement(name = OpenApiConfig.BASIC_AUTH)
public class TransactionSearchController {

    private final TransactionSearchService transactionSearchService;
    private final TransactionDecisionService transactionDecisionService;

    public TransactionSearchController(
            TransactionSearchService transactionSearchService,
            TransactionDecisionService transactionDecisionService) {
        this.transactionSearchService = transactionSearchService;
        this.transactionDecisionService = transactionDecisionService;
    }

    @GetMapping
    @Operation(
            summary = "Search transaction history",
            description = "Searches privacy-safe transaction records using optional filters, bounded "
                    + "pagination, and allow-listed sorting. Card references expose only the last "
                    + "four digits. **Required role: `SUPPORT`.**"
    )
    public TransactionSearchPage search(
            @Parameter(
                    description = "Original fraud decision",
                    example = "PROHIBITED",
                    schema = @Schema(
                            allowableValues = {"ALLOWED", "MANUAL_PROCESSING", "PROHIBITED"}
                    )
            )
            @RequestParam(value = "result", required = false) String result,
            @Parameter(
                    description = "Transaction region code",
                    example = "EAP",
                    schema = @Schema(
                            allowableValues = {"EAP", "ECA", "HIC", "LAC", "MENA", "SA", "SSA"}
                    )
            )
            @RequestParam(value = "region", required = false) String region,
            @Parameter(
                    description = "Whether specialist feedback is present",
                    example = "PENDING",
                    schema = @Schema(allowableValues = {"PENDING", "REVIEWED"})
            )
            @RequestParam(value = "feedbackStatus", required = false)
            String feedbackStatus,
            @Parameter(
                    description = "Inclusive start using yyyy-MM-dd'T'HH:mm:ss",
                    example = "2026-10-01T00:00:00"
            )
            @RequestParam(value = "from", required = false) String from,
            @Parameter(
                    description = "Inclusive end using yyyy-MM-dd'T'HH:mm:ss",
                    example = "2026-10-31T23:59:59"
            )
            @RequestParam(value = "to", required = false) String to,
            @Parameter(description = "Last four digits of the synthetic card", example = "3403")
            @RequestParam(value = "cardLast4", required = false)
            String cardLast4,
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size from 1 to 100", example = "20")
            @RequestParam(value = "size", defaultValue = "20") int size,
            @Parameter(
                    description = "Sort field: transactionId, amount, date, result, region, or feedback",
                    example = "date"
            )
            @RequestParam(value = "sort", defaultValue = "date") String sort,
            @Parameter(
                    description = "Sort direction",
                    example = "desc",
                    schema = @Schema(allowableValues = {"asc", "desc"})
            )
            @RequestParam(value = "direction", defaultValue = "desc")
            String direction) {

        return transactionSearchService.search(new SearchCriteria(
                result,
                region,
                feedbackStatus,
                from,
                to,
                cardLast4,
                page,
                size,
                sort,
                direction
        ));
    }

    @GetMapping("/{transactionId}/decision")
    @Operation(
            operationId = "inspectOriginalFraudDecision",
            summary = "Inspect the original fraud decision",
            description = "Returns the rule version, applied limits, reasons, and correlation counts "
                    + "captured at evaluation time. Evidence is never recomputed against today's "
                    + "limits. **Required role: `SUPPORT`.**"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Response body",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(
                            implementation = TransactionDecisionResponse.class
                    ),
                    examples = @ExampleObject(
                            name = "Amount-based manual review",
                            value = """
                                    {
                                      "transactionId": 7,
                                      "amount": 800,
                                      "result": "MANUAL_PROCESSING",
                                      "evidenceAvailable": true,
                                      "reasons": [
                                        "amount"
                                      ],
                                      "appliedLimits": {
                                        "maxAllowed": 320,
                                        "maxManual": 1500
                                      },
                                      "signals": {
                                        "suspiciousIp": false,
                                        "stolenCard": false,
                                        "differentIpCount": 0,
                                        "differentRegionCount": 0
                                      },
                                      "ruleVersion": "1.0"
                                    }
                                    """
                    )
            )
    )
    public TransactionDecisionResponse getDecision(
            @Parameter(description = "Stored transaction ID", example = "7")
            @PathVariable long transactionId) {

        return transactionDecisionService.getDecision(transactionId);
    }

}
