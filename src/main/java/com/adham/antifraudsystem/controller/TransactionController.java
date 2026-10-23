package com.adham.antifraudsystem.controller;

import com.adham.antifraudsystem.config.OpenApiConfig;
import com.adham.antifraudsystem.dto.TransactionDtos.TransactionRequest;
import com.adham.antifraudsystem.dto.TransactionDtos.TransactionResponse;
import com.adham.antifraudsystem.dto.TransactionDtos.FeedbackRequest;
import com.adham.antifraudsystem.dto.TransactionDtos.TransactionHistoryResponse;
import com.adham.antifraudsystem.service.TransactionFeedbackService;
import com.adham.antifraudsystem.service.TransactionHistoryService;
import com.adham.antifraudsystem.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/antifraud")
@SecurityRequirement(name = OpenApiConfig.BASIC_AUTH)
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionHistoryService transactionHistoryService;
    private final TransactionFeedbackService transactionFeedbackService;

    public TransactionController(
            TransactionService transactionService,
            TransactionHistoryService transactionHistoryService,
            TransactionFeedbackService transactionFeedbackService) {

        this.transactionService = transactionService;
        this.transactionHistoryService = transactionHistoryService;
        this.transactionFeedbackService = transactionFeedbackService;
    }

    @PostMapping("/transaction")
    @Operation(
            tags = "Transaction evaluation",
            summary = "Evaluate a transaction",
            description = "Evaluates amount, blacklist, IP-correlation, and region-correlation rules. "
                    + "The response contains the highest-severity decision and its alphabetically "
                    + "ordered reasons. Every valid attempt is stored. **Required role: `MERCHANT`.**"
    )
    public TransactionResponse processTransaction(
            @Valid @RequestBody TransactionRequest request) {
        return transactionService.evaluateTransaction(request);
    }

    @PutMapping("/transaction")
    @Operation(
            tags = "Transaction review",
            summary = "Submit transaction feedback",
            description = "Records a specialist's final assessment and updates the adaptive amount "
                    + "limits when applicable. Feedback can be submitted once, and it cannot equal "
                    + "the original result. **Required role: `SUPPORT`.**"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedback recorded and limits updated"),
            @ApiResponse(responseCode = "400", description = "Invalid feedback request"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "SUPPORT role required"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "409", description = "Feedback already exists"),
            @ApiResponse(
                    responseCode = "422",
                    description = "Feedback matches the original result",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    public TransactionHistoryResponse submitFeedback(
            @Valid @RequestBody FeedbackRequest request,
            @Parameter(hidden = true) Principal principal) {

        return transactionFeedbackService.submitFeedback(
                request,
                principal.getName()
        );
    }

    @GetMapping("/history")
    @Operation(
            tags = "Transaction review",
            summary = "List transaction history",
            description = "Returns every stored transaction in ascending ID order, including its "
                    + "immutable original result and any later feedback. **Required role: `SUPPORT`.**"
    )
    public List<TransactionHistoryResponse> getAllHistory() {
        return transactionHistoryService.getAllHistory();
    }

    @GetMapping("/history/{number}")
    @Operation(
            tags = "Transaction review",
            summary = "Get history for a card",
            description = "Returns stored transactions for one valid synthetic card number in "
                    + "ascending ID order. **Required role: `SUPPORT`.**"
    )
    public List<TransactionHistoryResponse> getHistoryByCard(
            @Parameter(description = "Synthetic card number", example = "4000008449433403")
            @PathVariable String number) {

        return transactionHistoryService.getHistoryByCard(number);
    }
}
