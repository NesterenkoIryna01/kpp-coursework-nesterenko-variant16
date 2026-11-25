package com.coursework.dao;

import com.coursework.model.RepairTask;
import com.coursework.util.DbConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RepairTaskDao {

    private static final Logger log = LoggerFactory.getLogger(RepairTaskDao.class);

    public List<RepairTask> findAll() {
        List<RepairTask> list = new ArrayList<>();
        String sql = """
            SELECT r.id, r.defect_id, r.title, r.description, r.assigned_to, r.planned_date,
                   r.completed_date, r.status, r.cost_estimate, r.created_at,
                   d.title AS defect_title
            FROM repair_task r
            LEFT JOIN defect d ON d.id = r.defect_id
            ORDER BY r.id
            """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            log.error("findAll RepairTask failed", e);
            throw new RuntimeException("Помилка отримання списку ремонтних завдань", e);
        }
        return list;
    }

    public Optional<RepairTask> findById(int id) {
        String sql = """
            SELECT r.id, r.defect_id, r.title, r.description, r.assigned_to, r.planned_date,
                   r.completed_date, r.status, r.cost_estimate, r.created_at,
                   d.title AS defect_title
            FROM repair_task r
            LEFT JOIN defect d ON d.id = r.defect_id
            WHERE r.id = ?
            """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            log.error("findById RepairTask id={}", id, e);
            throw new RuntimeException("Помилка пошуку ремонтного завдання", e);
        }
        return Optional.empty();
    }

    public RepairTask create(RepairTask t) {
        String sql = """
            INSERT INTO repair_task (defect_id, title, description, assigned_to, planned_date, completed_date, status, cost_estimate)
            VALUES (?,?,?,?,?,?,?,?)
            """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParams(ps, t);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    t.setId(keys.getInt(1));
                }
            }
            log.info("Created RepairTask id={}", t.getId());
            return t;
        } catch (SQLException e) {
            log.error("create RepairTask failed", e);
            throw new RuntimeException("Помилка створення ремонтного завдання", e);
        }
    }

    public boolean update(RepairTask t) {
        String sql = """
            UPDATE repair_task SET defect_id=?, title=?, description=?, assigned_to=?, planned_date=?,
                                   completed_date=?, status=?, cost_estimate=?
            WHERE id=?
            """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, t);
            ps.setInt(9, t.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("update RepairTask id={}", t.getId(), e);
            throw new RuntimeException("Помилка оновлення ремонтного завдання", e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM repair_task WHERE id=?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("delete RepairTask id={}", id, e);
            throw new RuntimeException("Помилка видалення ремонтного завдання", e);
        }
    }

    private void setParams(PreparedStatement ps, RepairTask t) throws SQLException {
        ps.setInt(1, t.getDefectId());
        ps.setString(2, t.getTitle());
        ps.setString(3, t.getDescription());
        ps.setString(4, t.getAssignedTo());
        if (t.getPlannedDate() != null) ps.setDate(5, Date.valueOf(t.getPlannedDate())); else ps.setNull(5, Types.DATE);
        if (t.getCompletedDate() != null) ps.setDate(6, Date.valueOf(t.getCompletedDate())); else ps.setNull(6, Types.DATE);
        ps.setString(7, t.getStatus() != null ? t.getStatus() : "PLANNED");
        if (t.getCostEstimate() != null) ps.setBigDecimal(8, t.getCostEstimate()); else ps.setNull(8, Types.DECIMAL);
    }

    private RepairTask map(ResultSet rs) throws SQLException {
        RepairTask t = new RepairTask();
        t.setId(rs.getInt("id"));
        t.setDefectId(rs.getInt("defect_id"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        t.setAssignedTo(rs.getString("assigned_to"));
        Date pd = rs.getDate("planned_date");
        if (pd != null) t.setPlannedDate(pd.toLocalDate());
        Date cd = rs.getDate("completed_date");
        if (cd != null) t.setCompletedDate(cd.toLocalDate());
        t.setStatus(rs.getString("status"));
        t.setCostEstimate(rs.getBigDecimal("cost_estimate"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) t.setCreatedAt(ts.toLocalDateTime());
        t.setDefectTitle(rs.getString("defect_title"));
        return t;
    }
}
