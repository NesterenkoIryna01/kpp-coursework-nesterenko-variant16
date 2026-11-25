package com.coursework.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DefectStateTest {

    @ParameterizedTest
    @CsvSource({
            "ЗАФІКСОВАНО, ПЕРЕВІРЕНО, true",
            "ПЕРЕВІРЕНО, РЕМОНТ_ПРИЗНАЧЕНО, true",
            "РЕМОНТ_ПРИЗНАЧЕНО, ЗАКРИТО, true",
            "ЗАФІКСОВАНО, ЗАКРИТО, false",
            "ЗАФІКСОВАНО, РЕМОНТ_ПРИЗНАЧЕНО, false",
            "ПЕРЕВІРЕНО, ЗАФІКСОВАНО, false",
            "ПЕРЕВІРЕНО, ЗАКРИТО, false",
            "РЕМОНТ_ПРИЗНАЧЕНО, ПЕРЕВІРЕНО, false",
            "ЗАКРИТО, ЗАФІКСОВАНО, false",
            "ЗАКРИТО, ПЕРЕВІРЕНО, false",
            "ЗАКРИТО, РЕМОНТ_ПРИЗНАЧЕНО, false",
            "ЗАКРИТО, ЗАКРИТО, false"
    })
    @DisplayName("Матриця допустимих переходів")
    void transitionMatrix(String from, String to, boolean expected) {
        assertEquals(expected, DefectState.valueOf(from).canTransitionTo(DefectState.valueOf(to)));
    }

    @Test
    @DisplayName("allowedNext для ЗАФІКСОВАНО містить лише ПЕРЕВІРЕНО")
    void allowedNext_fixed() {
        Set<DefectState> next = DefectState.allowedNext(DefectState.ЗАФІКСОВАНО);
        assertEquals(1, next.size());
        assertTrue(next.contains(DefectState.ПЕРЕВІРЕНО));
    }

    @Test
    @DisplayName("allowedNext для ЗАКРИТО порожній")
    void allowedNext_closed() {
        assertTrue(DefectState.allowedNext(DefectState.ЗАКРИТО).isEmpty());
    }

    @Test
    @DisplayName("canTransitionTo(null) = false")
    void transitionToNull() {
        assertFalse(DefectState.ЗАФІКСОВАНО.canTransitionTo(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ЗАФІКСОВАНО", "перевірено", "РЕМОНТ_ПРИЗНАЧЕНО", "закрито"})
    @DisplayName("fromString приймає валідні значення (регістр ігнорується)")
    void fromString_valid(String value) {
        assertDoesNotThrow(() -> DefectState.fromString(value));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "НЕВІДОМО", "OPEN", "123"})
    @DisplayName("fromString відхиляє невалідні значення")
    void fromString_invalid(String value) {
        assertThrows(IllegalArgumentException.class, () -> DefectState.fromString(value));
    }
}
