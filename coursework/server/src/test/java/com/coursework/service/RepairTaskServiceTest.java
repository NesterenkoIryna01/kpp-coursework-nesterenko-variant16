package com.coursework.service;

import com.coursework.model.RepairTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тести валідації RepairTaskService (без БД).
 */
class RepairTaskServiceTest {

    private RepairTaskService service;

    @BeforeEach
    void setUp() {
        service = new RepairTaskService();
    }

    @Test
    @DisplayName("Створення без defectId — помилка")
    void create_withoutDefect_throws() {
        RepairTask t = new RepairTask();
        t.setTitle("Ремонт фасаду");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(t));
        assertTrue(ex.getMessage().toLowerCase().contains("дефект"));
    }

    @Test
    @DisplayName("Створення без назви — помилка")
    void create_withoutTitle_throws() {
        RepairTask t = new RepairTask();
        t.setDefectId(1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(t));
        assertTrue(ex.getMessage().toLowerCase().contains("назв"));
    }

    @Test
    @DisplayName("Порожня назва — помилка")
    void create_blankTitle_throws() {
        RepairTask t = new RepairTask();
        t.setDefectId(1);
        t.setTitle("   ");
        assertThrows(IllegalArgumentException.class, () -> service.create(t));
    }

    @Test
    @DisplayName("Оновлення без ID — помилка")
    void update_withoutId_throws() {
        RepairTask t = new RepairTask();
        t.setDefectId(1);
        t.setTitle("Ремонт");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.update(t));
        assertTrue(ex.getMessage().toUpperCase().contains("ID") || ex.getMessage().toLowerCase().contains("id"));
    }
}
