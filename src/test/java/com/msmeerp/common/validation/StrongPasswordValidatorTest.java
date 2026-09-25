package com.msmeerp.common.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "Aa1!aaaaaa",   // exactly 10 chars, one of each class
            "Sup3r$ecureP@ss",
    })
    void acceptsPasswordsMeetingEveryRule(String password) {
        assertThat(validator.isValid(password, null)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "'', empty",
            "short1!A, too short (under 10 chars)",
            "alllowercase123!, missing uppercase",
            "ALLUPPERCASE123!, missing lowercase",
            "NoDigitsHere!!, missing digit",
            "NoSymbolsHere123, missing symbol",
    })
    void rejectsPasswordsMissingARequiredRule(String password, String reason) {
        assertThat(validator.isValid(password, null)).as(reason).isFalse();
    }

    @org.junit.jupiter.api.Test
    void rejectsNull() {
        assertThat(validator.isValid(null, null)).isFalse();
    }
}
