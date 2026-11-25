package com.coursework.servlet;

import com.coursework.model.Inspection;
import com.coursework.service.InspectionService;
import com.coursework.util.JsonUtil;
import com.google.gson.reflect.TypeToken;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

@WebServlet("/api/inspections/*")
public class InspectionServlet extends BaseServlet {

    private final InspectionService service = new InspectionService();

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
                                () -> { try { writeError(resp, 404, "Обстеження не знайдено"); } catch (IOException e) { throw new RuntimeException(e); } }
                        );
            }
        } catch (Exception e) {
            log.error("GET /api/inspections", e);
            writeError(resp, 500, "Внутрішня помилка сервера");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getPathInfo();
            // POST /api/inspections/import — пакетний імпорт
            if (path != null && path.equals("/import")) {
                Type listType = new TypeToken<List<Inspection>>(){}.getType();
                List<Inspection> items = JsonUtil.gson().fromJson(readBody(req), listType);
                int saved = service.importBatch(items != null ? items : List.of());
                writeJson(resp, 200, Map.of("imported", saved, "total", items != null ? items.size() : 0));
                return;
            }

            Inspection insp = JsonUtil.fromJson(readBody(req), Inspection.class);
            Inspection created = service.create(insp);
            writeJson(resp, 201, created);
        } catch (IllegalArgumentException e) {
            writeError(resp, 400, e.getMessage());
        } catch (Exception e) {
            log.error("POST /api/inspections", e);
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
            Inspection insp = JsonUtil.fromJson(readBody(req), Inspection.class);
            insp.setId(id);
            Inspection updated = service.update(insp);
            writeJson(resp, 200, updated);
        } catch (IllegalArgumentException e) {
            writeError(resp, 400, e.getMessage());
        } catch (Exception e) {
            log.error("PUT /api/inspections", e);
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
                writeError(resp, 404, "Обстеження не знайдено");
            }
        } catch (Exception e) {
            log.error("DELETE /api/inspections", e);
            writeError(resp, 500, "Внутрішня помилка сервера");
        }
    }
}
