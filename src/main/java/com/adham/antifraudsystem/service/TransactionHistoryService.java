package com.adham.antifraudsystem.service;

import com.adham.antifraudsystem.dto.TransactionDtos.TransactionHistoryResponse;
import com.adham.antifraudsystem.dto.TransactionDtos.TransactionRequest;
import com.adham.antifraudsystem.repository.TransactionRepository;
import com.adham.antifraudsystem.repository.TransactionRepository.NewTransaction;
import com.adham.antifraudsystem.repository.TransactionRepository.StoredTransaction;
import com.adham.antifraudsystem.validation.CardValidator;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TransactionHistoryService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

    private final TransactionRepository transactionRepository;

    public TransactionHistoryService(
            TransactionRepository transactionRepository) {

        this.transactionRepository = transactionRepository;
    }

    public void saveTransaction(
            TransactionRequest request,
            LocalDateTime transactionDate,
            FraudDecisionEngine.Decision decision,
            FraudDecisionEngine.Signals signals) {

        transactionRepository.save(new NewTransaction(
                request.amount(),
                request.ip(),
                request.number(),
                request.region(),
                transactionDate,
                decision.result().name(),
                decision.info(),
                signals.maxAllowed(),
                signals.maxManual(),
                signals.suspiciousIp(),
                signals.stolenCard(),
                signals.differentIpCount(),
                signals.differentRegionCount(),
                FraudDecisionEngine.RULE_VERSION
        ));
    }

    public long countDifferentIps(
            String cardNumber,
            String currentIp,
            LocalDateTime transactionDate) {

        return transactionRepository.countDifferentIps(
                cardNumber,
                currentIp,
                transactionDate
        );
    }

    public long countDifferentRegions(
            String cardNumber,
            String currentRegion,
            LocalDateTime transactionDate) {

        return transactionRepository.countDifferentRegions(
                cardNumber,
                currentRegion,
                transactionDate
        );
    }

    public List<TransactionHistoryResponse> getAllHistory() {
        return transactionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TransactionHistoryResponse> getHistoryByCard(String number) {
        if (!CardValidator.isValid(number)) {
            throw new IllegalArgumentException("Invalid card number");
        }

        List<TransactionHistoryResponse> transactions =
                transactionRepository.findByCard(number).stream()
                        .map(this::toResponse)
                        .toList();

        if (transactions.isEmpty()) {
            throw new NoSuchElementException(
                    "No transactions found for this card"
            );
        }
        return transactions;
    }

    public TransactionHistoryResponse getTransactionForFeedback(
            long transactionId) {

        return toResponse(
                transactionRepository.findByIdForUpdate(transactionId)
        );
    }

    public void saveFeedback(long transactionId, String feedback) {
        transactionRepository.updateFeedback(transactionId, feedback);
    }

    private TransactionHistoryResponse toResponse(
            StoredTransaction transaction) {

        return new TransactionHistoryResponse(
                transaction.id(),
                transaction.amount(),
                transaction.ip(),
                transaction.number(),
                transaction.region(),
                transaction.date().format(DATE_FORMATTER),
                transaction.result(),
                transaction.feedback()
        );
    }
}
