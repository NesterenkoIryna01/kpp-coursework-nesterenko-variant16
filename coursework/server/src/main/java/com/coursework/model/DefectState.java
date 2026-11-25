package com.coursework.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Чотиристанний життєвий цикл об’єкта «Дефект» (варіант 16).
 * <pre>
 *   ЗАФІКСОВАНО → ПЕРЕВІРЕНО → РЕМОНТ_ПРИЗНАЧЕНО → ЗАКРИТО
 * </pre>
 * Переходи дозволені лише вперед по ланцюжку. Зміна стану виконується
 * в {@code DefectService.changeState()}, а не довільним редагуванням поля.
 */
public enum DefectState {
    /** Дефект щойно зафіксовано (початковий стан). */
    ЗАФІКСОВАНО,
    /** Дефект перевірено спеціалістом. */
    ПЕРЕВІРЕНО,
    /** Призначено ремонтне завдання. */
    РЕМОНТ_ПРИЗНАЧЕНО,
    /** Роботи завершено, дефект закрито (кінцевий стан). */
    ЗАКРИТО;

    /**
     * Чи дозволений перехід з поточного стану в {@code next}.
     * Використовується Service-шаром перед оновленням у БД.
     */
    public boolean canTransitionTo(DefectState next) {
        if (next == null) return false;
        return switch (this) {
            case ЗАФІКСОВАНО -> next == ПЕРЕВІРЕНО;
            case ПЕРЕВІРЕНО -> next == РЕМОНТ_ПРИЗНАЧЕНО;
            case РЕМОНТ_ПРИЗНАЧЕНО -> next == ЗАКРИТО;
            case ЗАКРИТО -> false; // кінцевий стан — виходів немає
        };
    }

    /** Множина допустимих наступних станів (для UI-комбобоксу). */
    public static Set<DefectState> allowedNext(DefectState current) {
        return switch (current) {
            case ЗАФІКСОВАНО -> EnumSet.of(ПЕРЕВІРЕНО);
            case ПЕРЕВІРЕНО -> EnumSet.of(РЕМОНТ_ПРИЗНАЧЕНО);
            case РЕМОНТ_ПРИЗНАЧЕНО -> EnumSet.of(ЗАКРИТО);
            case ЗАКРИТО -> EnumSet.noneOf(DefectState.class);
        };
    }

    /**
     * Парсинг рядка зі значенням стану (регістр ігнорується).
     * @throws IllegalArgumentException якщо значення порожнє або невідоме
     */
    public static DefectState fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Стан дефекту не може бути порожнім");
        }
        try {
            return DefectState.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Невідомий стан дефекту: " + value);
        }
    }
}
