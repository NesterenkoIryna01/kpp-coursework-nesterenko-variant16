package com.coursework.service;

import com.coursework.dao.InspectionDao;
import com.coursework.model.Inspection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class InspectionService {

    private static final Logger log = LoggerFactory.getLogger(InspectionService.class);
    private final InspectionDao dao = new InspectionDao();

    public List<Inspection> findAll() {
        return dao.findAll();
    }

    public Optional<Inspection> findById(int id) {
        return dao.findById(id);
    }

    public Inspection create(Inspection insp) {
        validate(insp);
        log.info("Creating Inspection by {}", insp.getInspectorName());
        return dao.create(insp);
    }

    public Inspection update(Inspection insp) {
        if (insp.getId() == null) {
            throw new IllegalArgumentException("ID обов’язковий для оновлення");
        }
        validate(insp);
        if (!dao.update(insp)) {
            throw new IllegalArgumentException("Обстеження з id=" + insp.getId() + " не знайдено");
        }
        return dao.findById(insp.getId()).orElse(insp);
    }

    public boolean delete(int id) {
        return dao.delete(id);
    }

    public int importBatch(List<Inspection> items) {
        int success = 0;
        for (Inspection item : items) {
            try {
                validate(item);
                dao.create(item);
                success++;
            } catch (Exception e) {
                log.warn("Skipped invalid inspection row: {}", e.getMessage());
            }
        }
        log.info("CSV import finished: {} of {} records saved", success, items.size());
        return success;
    }

    private void validate(Inspection insp) {
        if (insp.getBuildingObjectId() == null) {
            throw new IllegalArgumentException("Потрібно вказати будівельний об’єкт");
        }
        if (insp.getInspectionDate() == null) {
            throw new IllegalArgumentException("Дата обстеження обов’язкова");
        }
        if (insp.getInspectorName() == null || insp.getInspectorName().isBlank()) {
            throw new IllegalArgumentException("ПІБ інспектора обов’язкове");
        }
    }
}
