CREATE INDEX transaction_history_date_id_idx
    ON transaction_history (transaction_date DESC, id DESC);

CREATE INDEX transaction_history_result_date_id_idx
    ON transaction_history (result, transaction_date DESC, id DESC);

CREATE INDEX transaction_history_region_date_id_idx
    ON transaction_history (region, transaction_date DESC, id DESC);

CREATE INDEX transaction_history_feedback_date_id_idx
    ON transaction_history (feedback, transaction_date DESC, id DESC);

CREATE INDEX transaction_history_card_last4_date_id_idx
    ON transaction_history ((RIGHT(number, 4)), transaction_date DESC, id DESC);
