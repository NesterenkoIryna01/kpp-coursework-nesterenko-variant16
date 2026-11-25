package com.coursework.service;

import com.coursework.dao.BuildingObjectDao;
import com.coursework.model.BuildingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class BuildingObjectService {

    private static final Logger log = LoggerFactory.getLogger(BuildingObjectService.class);
    private final BuildingObjectDao dao = new BuildingObjectDao();

    public List<BuildingObject> findAll() {
        return dao.findAll();
    }

    public Optional<BuildingObject> findById(int id) {
        return dao.findById(id);
    }

    public BuildingObject create(BuildingObject obj) {
        validate(obj);
        if (obj.getStatus() == null || obj.getStatus().isBlank()) {
            obj.setStatus("ACTIVE");
        }
        log.info("Creating BuildingObject '{}'", obj.getName());
        return dao.create(obj);
    }

    public BuildingObject update(BuildingObject obj) {
        if (obj.getId() == null) {
            throw new IllegalArgumentException("ID обов’язковий для оновлення");
        }
        validate(obj);
        if (!dao.update(obj)) {
            throw new IllegalArgumentException("Об’єкт з id=" + obj.getId() + " не знайдено");
        }
        return dao.findById(obj.getId()).orElse(obj);
    }

    public boolean delete(int id) {
        return dao.delete(id);
    }

    private void validate(BuildingObject obj) {
        if (obj.getName() == null || obj.getName().isBlank()) {
            throw new IllegalArgumentException("Назва об’єкта обов’язкова");
        }
        if (obj.getAddress() == null || obj.getAddress().isBlank()) {
            throw new IllegalArgumentException("Адреса обов’язкова");
        }
        if (obj.getObjectType() == null || obj.getObjectType().isBlank()) {
            throw new IllegalArgumentException("Тип об’єкта обов’язковий");
        }
    }
}
