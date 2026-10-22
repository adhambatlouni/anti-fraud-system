package com.adham.antifraudsystem.model;

public final class FraudTypes {

    private FraudTypes() {
    }

    public enum TransactionResult {
        ALLOWED,
        MANUAL_PROCESSING,
        PROHIBITED
    }

    public enum FraudReason {
        AMOUNT("amount"),
        CARD_NUMBER("card-number"),
        IP("ip"),
        IP_CORRELATION("ip-correlation"),
        REGION_CORRELATION("region-correlation");

        private final String apiValue;

        FraudReason(String apiValue) {
            this.apiValue = apiValue;
        }

        public String apiValue() {
            return apiValue;
        }
    }
}
