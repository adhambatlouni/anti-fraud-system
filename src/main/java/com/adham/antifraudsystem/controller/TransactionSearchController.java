package com.adham.antifraudsystem.controller;

import com.adham.antifraudsystem.config.OpenApiConfig;
import com.adham.antifraudsystem.dto.TransactionDtos.TransactionSearchPage;
import com.adham.antifraudsystem.dto.TransactionDtos.TransactionDecisionResponse;
import com.adham.antifraudsystem.service.TransactionDecisionService;
import com.adham.antifraudsystem.service.TransactionSearchService;
import com.adham.antifraudsystem.service.TransactionSearchService.SearchCriteria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/antifraud/transactions")
@Tag(
        name = "Transaction investigation",
        description = "Privacy-safe transaction search and decision evidence"
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
            description = "Required role: SUPPORT. Card references are masked."
    )
    public TransactionSearchPage search(
            @RequestParam(value = "result", required = false) String result,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "feedbackStatus", required = false)
            String feedbackStatus,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "cardLast4", required = false)
            String cardLast4,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "date") String sort,
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
            summary = "Inspect the original fraud decision",
            description = "Required role: SUPPORT. Returns the immutable "
                    + "evidence captured when the transaction was evaluated."
    )
    public TransactionDecisionResponse getDecision(
            @PathVariable long transactionId) {

        return transactionDecisionService.getDecision(transactionId);
    }

}
