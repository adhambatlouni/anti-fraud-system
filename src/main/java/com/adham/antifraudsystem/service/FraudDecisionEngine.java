package com.adham.antifraudsystem.service;

import com.adham.antifraudsystem.model.FraudTypes.FraudReason;
import com.adham.antifraudsystem.model.FraudTypes.TransactionResult;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FraudDecisionEngine {

    public static final String RULE_VERSION = "1.0";

    public Decision evaluate(Signals signals) {
        Set<FraudReason> prohibited = EnumSet.noneOf(FraudReason.class);
        Set<FraudReason> manual = EnumSet.noneOf(FraudReason.class);

        addAmountReason(signals, prohibited, manual);
        addBlacklistReasons(signals, prohibited);
        addCorrelationReason(
                signals.differentIpCount(),
                FraudReason.IP_CORRELATION,
                prohibited,
                manual
        );
        addCorrelationReason(
                signals.differentRegionCount(),
                FraudReason.REGION_CORRELATION,
                prohibited,
                manual
        );

        if (!prohibited.isEmpty()) {
            return new Decision(TransactionResult.PROHIBITED, join(prohibited));
        }
        if (!manual.isEmpty()) {
            return new Decision(TransactionResult.MANUAL_PROCESSING, join(manual));
        }
        return new Decision(TransactionResult.ALLOWED, "none");
    }

    private void addAmountReason(
            Signals signals,
            Set<FraudReason> prohibited,
            Set<FraudReason> manual) {

        if (signals.amount() > signals.maxManual()) {
            prohibited.add(FraudReason.AMOUNT);
        } else if (signals.amount() > signals.maxAllowed()) {
            manual.add(FraudReason.AMOUNT);
        }
    }

    private void addBlacklistReasons(Signals signals, Set<FraudReason> prohibited) {
        if (signals.stolenCard()) {
            prohibited.add(FraudReason.CARD_NUMBER);
        }
        if (signals.suspiciousIp()) {
            prohibited.add(FraudReason.IP);
        }
    }

    private void addCorrelationReason(
            long count,
            FraudReason reason,
            Set<FraudReason> prohibited,
            Set<FraudReason> manual) {

        if (count > 2) {
            prohibited.add(reason);
        } else if (count == 2) {
            manual.add(reason);
        }
    }

    private String join(Set<FraudReason> reasons) {
        return reasons.stream()
                .map(FraudReason::apiValue)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(", "));
    }

    public record Signals(
            long amount,
            long maxAllowed,
            long maxManual,
            boolean suspiciousIp,
            boolean stolenCard,
            long differentIpCount,
            long differentRegionCount) {
    }

    public record Decision(TransactionResult result, String info) {
    }
}
