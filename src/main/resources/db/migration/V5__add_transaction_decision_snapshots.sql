ALTER TABLE transaction_history
    ADD COLUMN decision_reasons VARCHAR(255),
    ADD COLUMN applied_max_allowed BIGINT,
    ADD COLUMN applied_max_manual BIGINT,
    ADD COLUMN suspicious_ip_at_decision BOOLEAN,
    ADD COLUMN stolen_card_at_decision BOOLEAN,
    ADD COLUMN different_ip_count BIGINT,
    ADD COLUMN different_region_count BIGINT,
    ADD COLUMN rule_version VARCHAR(20);

ALTER TABLE transaction_history
    ADD CONSTRAINT transaction_decision_counts_nonnegative_ck
        CHECK (
            different_ip_count IS NULL OR different_ip_count >= 0
        ) NOT VALID,
    ADD CONSTRAINT transaction_decision_regions_nonnegative_ck
        CHECK (
            different_region_count IS NULL OR different_region_count >= 0
        ) NOT VALID,
    ADD CONSTRAINT transaction_decision_limits_positive_ck
        CHECK (
            (applied_max_allowed IS NULL OR applied_max_allowed > 0)
            AND (applied_max_manual IS NULL OR applied_max_manual > 0)
        ) NOT VALID,
    ADD CONSTRAINT transaction_decision_snapshot_all_or_none_ck
        CHECK (
            (
                decision_reasons IS NULL
                AND applied_max_allowed IS NULL
                AND applied_max_manual IS NULL
                AND suspicious_ip_at_decision IS NULL
                AND stolen_card_at_decision IS NULL
                AND different_ip_count IS NULL
                AND different_region_count IS NULL
                AND rule_version IS NULL
            )
            OR
            (
                decision_reasons IS NOT NULL
                AND applied_max_allowed IS NOT NULL
                AND applied_max_manual IS NOT NULL
                AND suspicious_ip_at_decision IS NOT NULL
                AND stolen_card_at_decision IS NOT NULL
                AND different_ip_count IS NOT NULL
                AND different_region_count IS NOT NULL
                AND rule_version IS NOT NULL
            )
        ) NOT VALID;

ALTER TABLE transaction_history
    VALIDATE CONSTRAINT transaction_decision_counts_nonnegative_ck,
    VALIDATE CONSTRAINT transaction_decision_regions_nonnegative_ck,
    VALIDATE CONSTRAINT transaction_decision_limits_positive_ck,
    VALIDATE CONSTRAINT transaction_decision_snapshot_all_or_none_ck;

CREATE FUNCTION prevent_transaction_decision_snapshot_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.decision_reasons IS DISTINCT FROM OLD.decision_reasons
        OR NEW.applied_max_allowed IS DISTINCT FROM OLD.applied_max_allowed
        OR NEW.applied_max_manual IS DISTINCT FROM OLD.applied_max_manual
        OR NEW.suspicious_ip_at_decision
            IS DISTINCT FROM OLD.suspicious_ip_at_decision
        OR NEW.stolen_card_at_decision
            IS DISTINCT FROM OLD.stolen_card_at_decision
        OR NEW.different_ip_count IS DISTINCT FROM OLD.different_ip_count
        OR NEW.different_region_count
            IS DISTINCT FROM OLD.different_region_count
        OR NEW.rule_version IS DISTINCT FROM OLD.rule_version THEN
        RAISE EXCEPTION 'transaction decision snapshots are immutable';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER transaction_decision_snapshot_immutable
    BEFORE UPDATE ON transaction_history
    FOR EACH ROW
    EXECUTE FUNCTION prevent_transaction_decision_snapshot_mutation();
