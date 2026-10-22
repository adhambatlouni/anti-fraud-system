package com.adham.antifraudsystem.service;

import com.adham.antifraudsystem.dto.TransactionDtos.TransactionRequest;
import com.adham.antifraudsystem.dto.TransactionDtos.TransactionResponse;
import com.adham.antifraudsystem.model.Region;
import com.adham.antifraudsystem.validation.CardValidator;
import com.adham.antifraudsystem.validation.IpValidator;

import org.springframework.jdbc.core.JdbcTemplate;
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

    private final JdbcTemplate jdbcTemplate;
    private final TransactionHistoryService transactionHistoryService;
    private final FraudLimitService fraudLimitService;
    private final FraudDecisionEngine decisionEngine;

    public TransactionService(
            JdbcTemplate jdbcTemplate,
            TransactionHistoryService transactionHistoryService,
            FraudLimitService fraudLimitService,
            FraudDecisionEngine decisionEngine) {

        this.jdbcTemplate = jdbcTemplate;
        this.transactionHistoryService = transactionHistoryService;
        this.fraudLimitService = fraudLimitService;
        this.decisionEngine = decisionEngine;
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

        Long ipCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM suspicious_ips WHERE ip = ?",
                Long.class,
                request.ip()
        );

        boolean suspiciousIp = ipCount != null && ipCount > 0;

        Long cardCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stolen_cards WHERE number = ?",
                Long.class,
                request.number()
        );

        boolean stolenCard = cardCount != null && cardCount > 0;

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
