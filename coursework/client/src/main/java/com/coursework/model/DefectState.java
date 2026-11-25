package com.coursework.model;

import java.util.EnumSet;
import java.util.Set;

public enum DefectState {
    ЗАФІКСОВАНО,
    ПЕРЕВІРЕНО,
    РЕМОНТ_ПРИЗНАЧЕНО,
    ЗАКРИТО;

    public boolean canTransitionTo(DefectState next) {
        if (next == null) return false;
        return switch (this) {
            case ЗАФІКСОВАНО -> next == ПЕРЕВІРЕНО;
            case ПЕРЕВІРЕНО -> next == РЕМОНТ_ПРИЗНАЧЕНО;
            case РЕМОНТ_ПРИЗНАЧЕНО -> next == ЗАКРИТО;
            case ЗАКРИТО -> false;
        };
    }

    public static Set<DefectState> allowedNext(DefectState current) {
        return switch (current) {
            case ЗАФІКСОВАНО -> EnumSet.of(ПЕРЕВІРЕНО);
            case ПЕРЕВІРЕНО -> EnumSet.of(РЕМОНТ_ПРИЗНАЧЕНО);
            case РЕМОНТ_ПРИЗНАЧЕНО -> EnumSet.of(ЗАКРИТО);
            case ЗАКРИТО -> EnumSet.noneOf(DefectState.class);
        };
    }

    public static DefectState fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Стан дефекту не може бути порожнім");
        }
        return DefectState.valueOf(value.trim().toUpperCase());
    }
}
