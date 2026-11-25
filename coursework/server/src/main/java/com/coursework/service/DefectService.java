package com.coursework.service;

import com.coursework.dao.DefectDao;
import com.coursework.model.Defect;
import com.coursework.model.DefectState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Service-шар дефектів (предметна логіка).
 * <p>
 * Відповідальності (SOLID — Single Responsibility):
 * <ul>
 *   <li>валідація вхідних даних (фактори 0–100, обов’язкові поля);</li>
 *   <li>розрахунок інтегрального показника I;</li>
 *   <li>контроль допустимих переходів життєвого циклу;</li>
 *   <li>делегування SQL-операцій у {@link DefectDao}.</li>
 * </ul>
 * Servlet і JavaFX-клієнт не містять цих правил — лише викликають цей сервіс.
 */
public class DefectService {

    private static final Logger log = LoggerFactory.getLogger(DefectService.class);

    private final DefectDao dao;

    public DefectService() {
        this.dao = new DefectDao();
    }

    /** Конструктор для тестів / підміни DAO. */
    public DefectService(DefectDao dao) {
        this.dao = dao;
    }

    public List<Defect> findAll() {
        return dao.findAll();
    }

    public Optional<Defect> findById(int id) {
        return dao.findById(id);
    }

    public Defect create(Defect defect) {
        validate(defect);
        if (defect.getState() == null) {
            defect.setState(DefectState.ЗАФІКСОВАНО);
        }
        // I завжди перераховується на сервері — клієнту не довіряємо
        defect.setIntegralScore(calculateIntegral(
                defect.getSeverity(), defect.getAreaScale(), defect.getSafetyImpact()));
        log.info("Creating defect '{}'", defect.getTitle());
        return dao.create(defect);
    }

    public Defect update(Defect defect) {
        if (defect.getId() == null) {
            throw new IllegalArgumentException("ID дефекту обов’язковий для оновлення");
        }
        Optional<Defect> existing = dao.findById(defect.getId());
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Дефект з id=" + defect.getId() + " не знайдено");
        }
        validate(defect);
        // стан змінюється ТІЛЬКИ через changeState(), щоб не обійти перевірку переходів
        defect.setState(existing.get().getState());
        defect.setIntegralScore(calculateIntegral(
                defect.getSeverity(), defect.getAreaScale(), defect.getSafetyImpact()));
        dao.update(defect);
        return dao.findById(defect.getId()).orElse(defect);
    }

    /**
     * Зміна стану з перевіркою допустимості переходу.
     * Неприпустимий перехід (наприклад ЗАФІКСОВАНО → ЗАКРИТО) відхиляється.
     */
    public Defect changeState(int id, DefectState newState) {
        Optional<Defect> opt = dao.findById(id);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Дефект з id=" + id + " не знайдено");
        }
        Defect current = opt.get();
        if (!current.getState().canTransitionTo(newState)) {
            throw new IllegalArgumentException(
                    "Недопустимий перехід: " + current.getState() + " → " + newState +
                    ". Допустимі: " + DefectState.allowedNext(current.getState()));
        }
        dao.updateState(id, newState);
        log.info("Defect id={} state changed {} → {}", id, current.getState(), newState);
        return dao.findById(id).orElseThrow();
    }

    public boolean delete(int id) {
        return dao.delete(id);
    }

    /**
     * Інтегральний показник (єдина формула для всіх варіантів методички):
     * <pre>
     *   I = 0,40·F1 + 0,35·F2 + 0,25·F3
     * </pre>
     * де F1 — тяжкість дефекту, F2 — площа/масштаб, F3 — вплив на безпеку.
     * Усі фактори попередньо нормуються (clamp) до діапазону [0; 100].
     */
    public BigDecimal calculateIntegral(BigDecimal f1, BigDecimal f2, BigDecimal f3) {
        BigDecimal v1 = clamp(f1);
        BigDecimal v2 = clamp(f2);
        BigDecimal v3 = clamp(f3);
        return v1.multiply(new BigDecimal("0.40"))
                .add(v2.multiply(new BigDecimal("0.35")))
                .add(v3.multiply(new BigDecimal("0.25")))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Обрізає значення до [0; 100]; null трактується як 0. */
    private BigDecimal clamp(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        if (value.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        if (value.compareTo(new BigDecimal("100")) > 0) return new BigDecimal("100");
        return value;
    }

    private void validate(Defect d) {
        if (d.getTitle() == null || d.getTitle().isBlank()) {
            throw new IllegalArgumentException("Назва дефекту обов’язкова");
        }
        if (d.getBuildingObjectId() == null) {
            throw new IllegalArgumentException("Потрібно вказати будівельний об’єкт");
        }
        validateFactor(d.getSeverity(), "Тяжкість (F1)");
        validateFactor(d.getAreaScale(), "Площа/масштаб (F2)");
        validateFactor(d.getSafetyImpact(), "Вплив на безпеку (F3)");
    }

    private void validateFactor(BigDecimal value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " не може бути null");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(name + " має бути в діапазоні 0–100, отримано: " + value);
        }
    }
}
