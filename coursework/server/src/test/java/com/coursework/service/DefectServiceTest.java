package com.coursework.service;

import com.coursework.model.Defect;
import com.coursework.model.DefectState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тести предметної логіки DefectService (без БД).
 * Перевіряють формулу I, нормування, валідацію та життєвий цикл.
 */
class DefectServiceTest {

    private DefectService service;

    @BeforeEach
    void setUp() {
        // використовуємо реальний сервіс; методи calculateIntegral та canTransitionTo не потребують DAO
        service = new DefectService();
    }

    // ---------- Інтегральний показник ----------

    @Test
    @DisplayName("I = 0.40*F1 + 0.35*F2 + 0.25*F3 при максимальних значеннях")
    void calculateIntegral_maxValues() {
        BigDecimal result = service.calculateIntegral(
                new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("100"));
        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    @DisplayName("I = 0 при нульових факторах")
    void calculateIntegral_zeros() {
        BigDecimal result = service.calculateIntegral(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), result);
    }

    @Test
    @DisplayName("Типовий розрахунок I")
    void calculateIntegral_typical() {
        // F1=75, F2=40, F3=85 → 0.4*75 + 0.35*40 + 0.25*85 = 30 + 14 + 21.25 = 65.25
        BigDecimal result = service.calculateIntegral(
                new BigDecimal("75"), new BigDecimal("40"), new BigDecimal("85"));
        assertEquals(new BigDecimal("65.25"), result);
    }

    @Test
    @DisplayName("Clamp від'ємних значень до 0")
    void calculateIntegral_negativeClamped() {
        BigDecimal result = service.calculateIntegral(
                new BigDecimal("-10"), new BigDecimal("50"), new BigDecimal("50"));
        // 0*0.4 + 50*0.35 + 50*0.25 = 17.5 + 12.5 = 30.00
        assertEquals(new BigDecimal("30.00"), result);
    }

    @Test
    @DisplayName("Clamp значень >100 до 100")
    void calculateIntegral_over100Clamped() {
        BigDecimal result = service.calculateIntegral(
                new BigDecimal("150"), new BigDecimal("0"), new BigDecimal("0"));
        assertEquals(new BigDecimal("40.00"), result);
    }

    @Test
    @DisplayName("null фактори трактуються як 0")
    void calculateIntegral_nulls() {
        BigDecimal result = service.calculateIntegral(null, null, null);
        assertEquals(new BigDecimal("0.00"), result);
    }

    // ---------- Життєвий цикл ----------

    @Test
    @DisplayName("Допустимий перехід ЗАФІКСОВАНО → ПЕРЕВІРЕНО")
    void lifecycle_allowed_fixedToChecked() {
        assertTrue(DefectState.ЗАФІКСОВАНО.canTransitionTo(DefectState.ПЕРЕВІРЕНО));
    }

    @Test
    @DisplayName("Допустимий перехід ПЕРЕВІРЕНО → РЕМОНТ_ПРИЗНАЧЕНО")
    void lifecycle_allowed_checkedToAssigned() {
        assertTrue(DefectState.ПЕРЕВІРЕНО.canTransitionTo(DefectState.РЕМОНТ_ПРИЗНАЧЕНО));
    }

    @Test
    @DisplayName("Допустимий перехід РЕМОНТ_ПРИЗНАЧЕНО → ЗАКРИТО")
    void lifecycle_allowed_assignedToClosed() {
        assertTrue(DefectState.РЕМОНТ_ПРИЗНАЧЕНО.canTransitionTo(DefectState.ЗАКРИТО));
    }

    @Test
    @DisplayName("Недопустимий перехід ЗАФІКСОВАНО → ЗАКРИТО")
    void lifecycle_forbidden_skip() {
        assertFalse(DefectState.ЗАФІКСОВАНО.canTransitionTo(DefectState.ЗАКРИТО));
    }

    @Test
    @DisplayName("Недопустимий перехід із ЗАКРИТО")
    void lifecycle_forbidden_fromClosed() {
        assertFalse(DefectState.ЗАКРИТО.canTransitionTo(DefectState.ПЕРЕВІРЕНО));
        assertFalse(DefectState.ЗАКРИТО.canTransitionTo(DefectState.ЗАФІКСОВАНО));
    }

    @Test
    @DisplayName("Недопустимий зворотний перехід")
    void lifecycle_forbidden_backward() {
        assertFalse(DefectState.ПЕРЕВІРЕНО.canTransitionTo(DefectState.ЗАФІКСОВАНО));
        assertFalse(DefectState.РЕМОНТ_ПРИЗНАЧЕНО.canTransitionTo(DefectState.ПЕРЕВІРЕНО));
    }

    @Test
    @DisplayName("fromString коректно парсить стан")
    void state_fromString() {
        assertEquals(DefectState.ЗАФІКСОВАНО, DefectState.fromString("ЗАФІКСОВАНО"));
        assertEquals(DefectState.ПЕРЕВІРЕНО, DefectState.fromString("перевірено"));
    }

    @Test
    @DisplayName("fromString кидає виняток для невідомого стану")
    void state_fromString_invalid() {
        assertThrows(IllegalArgumentException.class, () -> DefectState.fromString("НЕВІДОМО"));
        assertThrows(IllegalArgumentException.class, () -> DefectState.fromString(""));
        assertThrows(IllegalArgumentException.class, () -> DefectState.fromString(null));
    }

    // ---------- Валідація через calculate + модель ----------

    @Test
    @DisplayName("Модель Defect.calculateIntegralScore узгоджена з сервісом")
    void model_calculateMatchesService() {
        Defect d = new Defect();
        d.setSeverity(new BigDecimal("60"));
        d.setAreaScale(new BigDecimal("80"));
        d.setSafetyImpact(new BigDecimal("20"));
        BigDecimal fromModel = d.calculateIntegralScore();
        BigDecimal fromService = service.calculateIntegral(d.getSeverity(), d.getAreaScale(), d.getSafetyImpact());
        assertEquals(fromService, fromModel);
    }

    @Test
    @DisplayName("Ваги: лише F1=100 дає I=40")
    void calculateIntegral_onlyF1() {
        assertEquals(new BigDecimal("40.00"),
                service.calculateIntegral(new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Ваги: лише F2=100 дає I=35")
    void calculateIntegral_onlyF2() {
        assertEquals(new BigDecimal("35.00"),
                service.calculateIntegral(BigDecimal.ZERO, new BigDecimal("100"), BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Ваги: лише F3=100 дає I=25")
    void calculateIntegral_onlyF3() {
        assertEquals(new BigDecimal("25.00"),
                service.calculateIntegral(BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100")));
    }

    @Test
    @DisplayName("Округлення HALF_UP: 0.40*33 + 0.35*33 + 0.25*33 = 33.00")
    void calculateIntegral_rounding() {
        BigDecimal v = new BigDecimal("33");
        assertEquals(new BigDecimal("33.00"), service.calculateIntegral(v, v, v));
    }

    @Test
    @DisplayName("Межові значення 0 і 100 одночасно")
    void calculateIntegral_boundaries() {
        BigDecimal result = service.calculateIntegral(
                BigDecimal.ZERO, new BigDecimal("100"), BigDecimal.ZERO);
        assertEquals(new BigDecimal("35.00"), result);
        assertTrue(result.compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(result.compareTo(new BigDecimal("100")) <= 0);
    }
}
