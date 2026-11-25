package com.coursework.service;

import com.coursework.model.BuildingObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тести валідації BuildingObjectService (без звернення до БД).
 * Методи create/update викликають DAO, тому перевіряємо лише валідацію через
 * очікувані IllegalArgumentException до звернення до БД.
 */
class BuildingObjectServiceTest {

    private BuildingObjectService service;

    @BeforeEach
    void setUp() {
        service = new BuildingObjectService();
    }

    @Test
    @DisplayName("Створення без назви — помилка валідації")
    void create_withoutName_throws() {
        BuildingObject obj = new BuildingObject();
        obj.setAddress("вул. Тестова, 1");
        obj.setObjectType("Житловий");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(obj));
        assertTrue(ex.getMessage().toLowerCase().contains("назв"));
    }

    @Test
    @DisplayName("Створення без адреси — помилка валідації")
    void create_withoutAddress_throws() {
        BuildingObject obj = new BuildingObject();
        obj.setName("Тест");
        obj.setObjectType("Житловий");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(obj));
        assertTrue(ex.getMessage().toLowerCase().contains("адрес"));
    }

    @Test
    @DisplayName("Створення без типу — помилка валідації")
    void create_withoutType_throws() {
        BuildingObject obj = new BuildingObject();
        obj.setName("Тест");
        obj.setAddress("вул. Тестова, 1");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.create(obj));
        assertTrue(ex.getMessage().toLowerCase().contains("тип"));
    }

    @Test
    @DisplayName("Оновлення без ID — помилка")
    void update_withoutId_throws() {
        BuildingObject obj = new BuildingObject();
        obj.setName("Тест");
        obj.setAddress("вул. Тестова, 1");
        obj.setObjectType("Житловий");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.update(obj));
        assertTrue(ex.getMessage().toUpperCase().contains("ID") || ex.getMessage().toLowerCase().contains("id"));
    }

    @Test
    @DisplayName("Порожня назва (пробіли) — помилка")
    void create_blankName_throws() {
        BuildingObject obj = new BuildingObject();
        obj.setName("   ");
        obj.setAddress("вул. Тестова, 1");
        obj.setObjectType("Житловий");
        assertThrows(IllegalArgumentException.class, () -> service.create(obj));
    }
}
