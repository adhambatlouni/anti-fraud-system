package com.adham.antifraudsystem.validation;

public final class CardValidator {

    private CardValidator() {
    }

    public static boolean isValid(String number) {

        if (number == null || !number.matches("[0-9]{16}")) {
            return false;
        }

        int sum = 0;
        boolean doubleDigit = false;

        for (int i = number.length() - 1; i >= 0; i--) {

            int digit = number.charAt(i) - '0';

            if (doubleDigit) {
                digit *= 2;

                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            doubleDigit = !doubleDigit;
        }

        return sum % 10 == 0;
    }

    public static String mask(String number) {
        if (number == null || number.length() < 4) {
            throw new IllegalArgumentException("Invalid card number");
        }

        return "*".repeat(number.length() - 4)
                + number.substring(number.length() - 4);
    }
}
