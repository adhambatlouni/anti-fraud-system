package com.adham.antifraudsystem.service;

import com.adham.antifraudsystem.dto.TransactionDtos.TransactionRequest;
import com.adham.antifraudsystem.dto.TransactionDtos.TransactionResponse;
import com.adham.antifraudsystem.model.Region;
import com.adham.antifraudsystem.repository.BlacklistRepository;
import com.adham.antifraudsystem.validation.CardValidator;
import com.adham.antifraudsystem.validation.IpValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

@Service
public class TransactionService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss")
                    .withResolverStyle(ResolverStyle.STRICT);

    private final TransactionHistoryService transactionHistoryService;
    private final FraudLimitService fraudLimitService;
    private final FraudDecisionEngine decisionEngine;
    private final BlacklistRepository blacklistRepository;

    public TransactionService(
            TransactionHistoryService transactionHistoryService,
            FraudLimitService fraudLimitService,
            FraudDecisionEngine decisionEngine,
            BlacklistRepository blacklistRepository) {

        this.transactionHistoryService = transactionHistoryService;
        this.fraudLimitService = fraudLimitService;
        this.decisionEngine = decisionEngine;
        this.blacklistRepository = blacklistRepository;
    }

    @Transactional
    public TransactionResponse evaluateTransaction(
            TransactionRequest request) {

        if (request == null
                || !IpValidator.isValid(request.ip())
                || !CardValidator.isValid(request.number())
                || !Region.isValid(request.region())) {

            throw new IllegalArgumentException(
                    "Invalid transaction data"
            );
        }

        if (request.date() == null
                || !request.date().matches(
                "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"
        )) {

            throw new IllegalArgumentException(
                    "Invalid transaction date"
            );
        }

        LocalDateTime transactionDate;

        try {
            transactionDate = LocalDateTime.parse(
                    request.date(),
                    DATE_FORMATTER
            );

        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Invalid transaction date",
                    ex
            );
        }

        long amount = request.amount();

        FraudLimitService.Limits limits =
                fraudLimitService.getCurrentLimits();

        long maxAllowed = limits.maxAllowed();
        long maxManual = limits.maxManual();

        boolean suspiciousIp = blacklistRepository.containsIp(
                request.ip()
        );
        boolean stolenCard = blacklistRepository.containsCard(
                request.number()
        );

        long differentIpCount =
                transactionHistoryService.countDifferentIps(
                        request.number(),
                        request.ip(),
                        transactionDate
                );

        long differentRegionCount =
                transactionHistoryService.countDifferentRegions(
                        request.number(),
                        request.region(),
                        transactionDate
                );

        FraudDecisionEngine.Signals signals =
                new FraudDecisionEngine.Signals(
                        amount,
                        maxAllowed,
                        maxManual,
                        suspiciousIp,
                        stolenCard,
                        differentIpCount,
                        differentRegionCount
                );

        FraudDecisionEngine.Decision decision =
                decisionEngine.evaluate(signals);

        TransactionResponse response = new TransactionResponse(
                decision.result().name(),
                decision.info()
        );

        transactionHistoryService.saveTransaction(
                request,
                transactionDate,
                decision,
                signals
        );

        return response;
    }
}
