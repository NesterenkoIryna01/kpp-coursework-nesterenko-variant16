package com.coursework.servlet;

import com.coursework.model.RepairTask;
import com.coursework.service.RepairTaskService;
import com.coursework.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@WebServlet("/api/repairs/*")
public class RepairTaskServlet extends BaseServlet {

    private final RepairTaskService service = new RepairTaskService();

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
                                () -> { try { writeError(resp, 404, "Завдання не знайдено"); } catch (IOException e) { throw new RuntimeException(e); } }
                        );
            }
        } catch (Exception e) {
            log.error("GET /api/repairs", e);
            writeError(resp, 500, "Внутрішня помилка сервера");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            RepairTask task = JsonUtil.fromJson(readBody(req), RepairTask.class);
            RepairTask created = service.create(task);
            writeJson(resp, 201, created);
        } catch (IllegalArgumentException e) {
            writeError(resp, 400, e.getMessage());
        } catch (Exception e) {
            log.error("POST /api/repairs", e);
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
            RepairTask task = JsonUtil.fromJson(readBody(req), RepairTask.class);
            task.setId(id);
            RepairTask updated = service.update(task);
            writeJson(resp, 200, updated);
        } catch (IllegalArgumentException e) {
            writeError(resp, 400, e.getMessage());
        } catch (Exception e) {
            log.error("PUT /api/repairs", e);
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
                writeError(resp, 404, "Завдання не знайдено");
            }
        } catch (Exception e) {
            log.error("DELETE /api/repairs", e);
            writeError(resp, 500, "Внутрішня помилка сервера");
        }
    }
}
