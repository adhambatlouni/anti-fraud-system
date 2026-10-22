package com.adham.antifraudsystem.service;

import com.adham.antifraudsystem.dto.TransactionDtos.FeedbackRequest;
import com.adham.antifraudsystem.dto.TransactionDtos.TransactionHistoryResponse;
import com.adham.antifraudsystem.model.FraudTypes.TransactionResult;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.adham.antifraudsystem.service.AuditService.Action.FEEDBACK_SUBMITTED;
import static com.adham.antifraudsystem.service.AuditService.Action.LIMIT_UPDATED;
import static com.adham.antifraudsystem.service.AuditService.ResourceType.TRANSACTION;
import static com.adham.antifraudsystem.service.AuditService.ResourceType.TRANSACTION_LIMITS;

@Service
public class TransactionFeedbackService {

    private final TransactionHistoryService historyService;
    private final FraudLimitService fraudLimitService;
    private final AuditService auditService;

    public TransactionFeedbackService(
            TransactionHistoryService historyService,
            FraudLimitService fraudLimitService,
            AuditService auditService) {

        this.historyService = historyService;
        this.fraudLimitService = fraudLimitService;
        this.auditService = auditService;
    }

    @Transactional
    public TransactionHistoryResponse submitFeedback(
            FeedbackRequest request,
            String actorUsername) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Invalid feedback request"
            );
        }

        TransactionResult feedback = parseFeedback(request.feedback());

        TransactionHistoryResponse transaction =
                historyService.getTransactionForFeedback(
                        request.transactionId()
                );

        if (!transaction.feedback().isEmpty()) {
            throw new DuplicateFeedbackException();
        }

        TransactionResult original = parseStoredResult(transaction.result());

        if (original == feedback) {
            throw new InvalidFeedbackCombinationException();
        }

        FraudLimitService.LimitChange limitChange =
                fraudLimitService.applyFeedback(
                original,
                feedback,
                transaction.amount()
        );

        historyService.saveFeedback(
                request.transactionId(),
                feedback.name()
        );

        auditService.record(
                actorUsername,
                FEEDBACK_SUBMITTED,
                TRANSACTION,
                transaction.transactionId().toString(),
                original.name(),
                feedback.name()
        );

        if (!limitChange.before().equals(limitChange.after())) {
            auditService.record(
                    actorUsername,
                    LIMIT_UPDATED,
                    TRANSACTION_LIMITS,
                    "1",
                    describe(limitChange.before()),
                    describe(limitChange.after())
            );
        }

        return new TransactionHistoryResponse(
                transaction.transactionId(),
                transaction.amount(),
                transaction.ip(),
                transaction.number(),
                transaction.region(),
                transaction.date(),
                transaction.result(),
                feedback.name()
        );
    }

    private String describe(FraudLimitService.Limits limits) {
        return "maxAllowed=%d,maxManual=%d".formatted(
                limits.maxAllowed(),
                limits.maxManual()
        );
    }

    private TransactionResult parseFeedback(String feedback) {
        try {
            return TransactionResult.valueOf(feedback);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalArgumentException("Invalid feedback request", exception);
        }
    }

    private TransactionResult parseStoredResult(String result) {
        try {
            return TransactionResult.valueOf(result);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalStateException("Unknown transaction result", exception);
        }
    }

    public static class DuplicateFeedbackException
            extends RuntimeException {

        public DuplicateFeedbackException() {
            super("Feedback already exists");
        }
    }

    public static class InvalidFeedbackCombinationException
            extends RuntimeException {

        public InvalidFeedbackCombinationException() {
            super("Feedback matches the original result");
        }
    }
}
