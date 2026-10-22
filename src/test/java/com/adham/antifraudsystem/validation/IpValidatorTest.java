package com.adham.antifraudsystem.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class IpValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "0.0.0.0",
            "127.0.0.1",
            "192.168.1.1",
            "255.255.255.255"
    })
    void acceptsValidIpv4Addresses(String ip) {
        assertThat(IpValidator.isValid(ip)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "1.2.3",
            "1.2.3.4.5",
            "256.1.1.1",
            "01.2.3.4",
            "1..3.4",
            "-1.2.3.4",
            "a.b.c.d",
            " 192.168.1.1"
    })
    void rejectsInvalidIpv4Addresses(String ip) {
        assertThat(IpValidator.isValid(ip)).isFalse();
    }
}
