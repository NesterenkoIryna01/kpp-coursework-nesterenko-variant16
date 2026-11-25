package com.coursework.service;

import com.coursework.model.Inspection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тести валідації InspectionService (без БД).
 */
class InspectionServiceTest {

    private InspectionService service;

    @BeforeEach
    void setUp() {
        service = new InspectionService();
    }

    @Test
    @DisplayName("Створення без buildingObjectId — помилка")
    void create_withoutBuilding_throws() {
        Inspection insp = new Inspection();
        insp.setInspectionDate(LocalDate.now());
        insp.setInspectorName("Тест Інспектор");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(insp));
        assertTrue(ex.getMessage().toLowerCase().contains("об’єкт") || ex.getMessage().toLowerCase().contains("об'єкт")
                || ex.getMessage().toLowerCase().contains("будівельн"));
    }

    @Test
    @DisplayName("Створення без дати — помилка")
    void create_withoutDate_throws() {
        Inspection insp = new Inspection();
        insp.setBuildingObjectId(1);
        insp.setInspectorName("Тест Інспектор");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(insp));
        assertTrue(ex.getMessage().toLowerCase().contains("дата"));
    }

    @Test
    @DisplayName("Створення без інспектора — помилка")
    void create_withoutInspector_throws() {
        Inspection insp = new Inspection();
        insp.setBuildingObjectId(1);
        insp.setInspectionDate(LocalDate.now());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(insp));
        assertTrue(ex.getMessage().toLowerCase().contains("інспектор") || ex.getMessage().toLowerCase().contains("піб"));
    }

    @Test
    @DisplayName("Порожнє ім’я інспектора — помилка")
    void create_blankInspector_throws() {
        Inspection insp = new Inspection();
        insp.setBuildingObjectId(1);
        insp.setInspectionDate(LocalDate.now());
        insp.setInspectorName("  ");
        assertThrows(IllegalArgumentException.class, () -> service.create(insp));
    }

    @Test
    @DisplayName("Оновлення без ID — помилка")
    void update_withoutId_throws() {
        Inspection insp = new Inspection();
        insp.setBuildingObjectId(1);
        insp.setInspectionDate(LocalDate.now());
        insp.setInspectorName("Тест");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.update(insp));
        assertTrue(ex.getMessage().toUpperCase().contains("ID") || ex.getMessage().toLowerCase().contains("id"));
    }
}
