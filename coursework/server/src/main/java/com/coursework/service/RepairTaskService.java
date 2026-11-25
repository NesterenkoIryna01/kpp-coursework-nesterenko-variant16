package com.coursework.service;

import com.coursework.dao.RepairTaskDao;
import com.coursework.model.RepairTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class RepairTaskService {

    private static final Logger log = LoggerFactory.getLogger(RepairTaskService.class);
    private final RepairTaskDao dao = new RepairTaskDao();

    public List<RepairTask> findAll() {
        return dao.findAll();
    }

    public Optional<RepairTask> findById(int id) {
        return dao.findById(id);
    }

    public RepairTask create(RepairTask task) {
        validate(task);
        if (task.getStatus() == null || task.getStatus().isBlank()) {
            task.setStatus("PLANNED");
        }
        log.info("Creating RepairTask '{}'", task.getTitle());
        return dao.create(task);
    }

    public RepairTask update(RepairTask task) {
        if (task.getId() == null) {
            throw new IllegalArgumentException("ID обов’язковий для оновлення");
        }
        validate(task);
        if (!dao.update(task)) {
            throw new IllegalArgumentException("Завдання з id=" + task.getId() + " не знайдено");
        }
        return dao.findById(task.getId()).orElse(task);
    }

    public boolean delete(int id) {
        return dao.delete(id);
    }

    private void validate(RepairTask task) {
        if (task.getDefectId() == null) {
            throw new IllegalArgumentException("Потрібно вказати дефект");
        }
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            throw new IllegalArgumentException("Назва завдання обов’язкова");
        }
    }
}
