package com.coursework.servlet;

import com.coursework.model.BuildingObject;
import com.coursework.service.BuildingObjectService;
import com.coursework.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@WebServlet("/api/buildings/*")
public class BuildingObjectServlet extends BaseServlet {

    private final BuildingObjectService service = new BuildingObjectService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Integer id = parseId(req);
            if (id == null) {
                writeJson(resp, 200, service.findAll());
            } else {
                service.findById(id)
                        .ifPresentOrElse(
                                o -> { try { writeJson(resp, 200, o); } catch (IOException e) { throw new RuntimeException(e); } },
                                () -> { try { writeError(resp, 404, "Об’єкт не знайдено"); } catch (IOException e) { throw new RuntimeException(e); } }
                        );
            }
        } catch (Exception e) {
            log.error("GET /api/buildings", e);
            writeError(resp, 500, "Внутрішня помилка сервера");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            BuildingObject obj = JsonUtil.fromJson(readBody(req), BuildingObject.class);
            BuildingObject created = service.create(obj);
            writeJson(resp, 201, created);
        } catch (IllegalArgumentException e) {
            writeError(resp, 400, e.getMessage());
        } catch (Exception e) {
            log.error("POST /api/buildings", e);
            writeError(resp, 500, "Внутрішня помилка сервера");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Integer id = parseId(req);
            if (id == null) {
                writeError(resp, 400, "ID обов’язковий");
                return;
            }
            BuildingObject obj = JsonUtil.fromJson(readBody(req), BuildingObject.class);
            obj.setId(id);
            BuildingObject updated = service.update(obj);
            writeJson(resp, 200, updated);
        } catch (IllegalArgumentException e) {
            writeError(resp, 400, e.getMessage());
        } catch (Exception e) {
            log.error("PUT /api/buildings", e);
            writeError(resp, 500, "Внутрішня помилка сервера");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Integer id = parseId(req);
            if (id == null) {
                writeError(resp, 400, "ID обов’язковий");
                return;
            }
            boolean deleted = service.delete(id);
            if (deleted) {
                writeJson(resp, 200, Map.of("message", "Видалено"));
            } else {
                writeError(resp, 404, "Об’єкт не знайдено");
            }
        } catch (Exception e) {
            log.error("DELETE /api/buildings", e);
            writeError(resp, 500, "Внутрішня помилка сервера");
        }
    }
}
