CREATE INDEX transaction_history_card_time_idx
    ON transaction_history (number, transaction_date)
    INCLUDE (ip, region);

CREATE INDEX transaction_history_card_id_idx
    ON transaction_history (number, id);
