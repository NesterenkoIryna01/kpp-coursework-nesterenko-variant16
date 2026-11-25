package com.coursework.servlet;

import com.coursework.model.Defect;
import com.coursework.model.DefectState;
import com.coursework.service.DefectService;
import com.coursework.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@WebServlet("/api/defects/*")
public class DefectServlet extends BaseServlet {

    private final DefectService service = new DefectService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Integer id = parseId(req);
            if (id == null) {
                writeJson(resp, 200, service.findAll());
            } else {
                service.findById(id)
                        .ifPresentOrElse(
                                d -> {
                                    try { writeJson(resp, 200, d); } catch (IOException e) { throw new RuntimeException(e); }
                                },
                                () -> {
                                    try { writeError(resp, 404, "Дефект не знайдено"); } catch (IOException e) { throw new RuntimeException(e); }
                                }
                        );
            }
        } catch (Exception e) {
            log.error("GET /api/defects", e);
            writeError(resp, 500, "Внутрішня помилка сервера");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getPathInfo();
            // POST /api/defects/{id}/state  — зміна стану
            if (path != null && path.matches("/\\d+/state")) {
                Integer id = parseId(req);
                Map<?, ?> body = JsonUtil.fromJson(readBody(req), Map.class);
                String stateStr = body != null ? String.valueOf(body.get("state")) : null;
                DefectState newState = DefectState.fromString(stateStr);
                Defect updated = service.changeState(id, newState);
                writeJson(resp, 200, updated);
                return;
            }

            Defect defect = JsonUtil.fromJson(readBody(req), Defect.class);
            Defect created = service.create(defect);
            writeJson(resp, 201, created);
        } catch (IllegalArgumentException e) {
            writeError(resp, 400, e.getMessage());
        } catch (Exception e) {
            log.error("POST /api/defects", e);
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
            Defect defect = JsonUtil.fromJson(readBody(req), Defect.class);
            defect.setId(id);
            Defect updated = service.update(defect);
            writeJson(resp, 200, updated);
        } catch (IllegalArgumentException e) {
            writeError(resp, 400, e.getMessage());
        } catch (Exception e) {
            log.error("PUT /api/defects", e);
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
                writeError(resp, 404, "Дефект не знайдено");
            }
        } catch (Exception e) {
            log.error("DELETE /api/defects", e);
            writeError(resp, 500, "Внутрішня помилка сервера");
        }
    }
}
