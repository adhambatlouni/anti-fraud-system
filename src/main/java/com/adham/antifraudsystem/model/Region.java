package com.adham.antifraudsystem.model;

public enum Region {
    EAP,
    ECA,
    HIC,
    LAC,
    MENA,
    SA,
    SSA;

    public static boolean isValid(String value) {

        if (value == null) {
            return false;
        }

        for (Region region : values()) {
            if (region.name().equals(value)) {
                return true;
            }
        }

        return false;
    }
}