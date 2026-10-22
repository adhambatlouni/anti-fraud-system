package com.adham.antifraudsystem.service;

import com.adham.antifraudsystem.model.FraudTypes.TransactionResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FraudDecisionEngineTest {

    private final FraudDecisionEngine engine = new FraudDecisionEngine();

    @Test
    void allowsTransactionWithoutRiskSignals() {
        FraudDecisionEngine.Decision decision = evaluate(100, false, false, 0, 0);

        assertThat(decision.result()).isEqualTo(TransactionResult.ALLOWED);
        assertThat(decision.info()).isEqualTo("none");
    }

    @Test
    void combinesManualReasonsInAlphabeticalOrder() {
        FraudDecisionEngine.Decision decision = evaluate(300, false, false, 2, 2);

        assertThat(decision.result()).isEqualTo(TransactionResult.MANUAL_PROCESSING);
        assertThat(decision.info())
                .isEqualTo("amount, ip-correlation, region-correlation");
    }

    @Test
    void combinesProhibitedReasonsInAlphabeticalOrder() {
        FraudDecisionEngine.Decision decision = evaluate(1_501, true, true, 3, 3);

        assertThat(decision.result()).isEqualTo(TransactionResult.PROHIBITED);
        assertThat(decision.info())
                .isEqualTo("amount, card-number, ip, ip-correlation, region-correlation");
    }

    @Test
    void reportsOnlyHighestSeverityReasons() {
        FraudDecisionEngine.Decision decision = evaluate(300, true, false, 2, 0);

        assertThat(decision.result()).isEqualTo(TransactionResult.PROHIBITED);
        assertThat(decision.info()).isEqualTo("ip");
    }

    @Test
    void treatsTwoCorrelationsAsManualAndMoreThanTwoAsProhibited() {
        FraudDecisionEngine.Decision manual = evaluate(100, false, false, 2, 0);
        FraudDecisionEngine.Decision prohibited = evaluate(100, false, false, 3, 0);

        assertThat(manual.result()).isEqualTo(TransactionResult.MANUAL_PROCESSING);
        assertThat(prohibited.result()).isEqualTo(TransactionResult.PROHIBITED);
    }

    private FraudDecisionEngine.Decision evaluate(
            long amount,
            boolean suspiciousIp,
            boolean stolenCard,
            long differentIpCount,
            long differentRegionCount) {

        return engine.evaluate(new FraudDecisionEngine.Signals(
                amount,
                200,
                1_500,
                suspiciousIp,
                stolenCard,
                differentIpCount,
                differentRegionCount
        ));
    }
}
