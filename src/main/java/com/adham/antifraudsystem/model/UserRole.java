package com.adham.antifraudsystem.model;

public enum UserRole {
    ADMINISTRATOR,
    MERCHANT,
    SUPPORT;

    public String getAuthority() {
        return "ROLE_" + name();
    }
}