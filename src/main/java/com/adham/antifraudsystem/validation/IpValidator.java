package com.adham.antifraudsystem.validation;

public final class IpValidator {

    private IpValidator() {
    }

    public static boolean isValid(String ip) {

        if (ip == null) {
            return false;
        }

        String[] octets = ip.split("\\.", -1);

        if (octets.length != 4) {
            return false;
        }

        for (String octet : octets) {

            if (!octet.matches("0|[1-9][0-9]{0,2}")) {
                return false;
            }

            int value = Integer.parseInt(octet);

            if (value > 255) {
                return false;
            }
        }

        return true;
    }
}