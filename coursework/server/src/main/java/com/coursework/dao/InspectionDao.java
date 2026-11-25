package com.coursework.dao;

import com.coursework.model.Inspection;
import com.coursework.util.DbConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InspectionDao {

    private static final Logger log = LoggerFactory.getLogger(InspectionDao.class);

    public List<Inspection> findAll() {
        List<Inspection> list = new ArrayList<>();
        String sql = """
            SELECT i.id, i.building_object_id, i.defect_id, i.inspection_date, i.inspector_name,
                   i.findings, i.measured_value, i.unit, i.notes, i.created_at,
                   b.name AS building_name
            FROM inspection i
            LEFT JOIN building_object b ON b.id = i.building_object_id
            ORDER BY i.inspection_date DESC, i.id DESC
            """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            log.error("findAll Inspection failed", e);
            throw new RuntimeException("Помилка отримання списку обстежень", e);
        }
        return list;
    }

    public Optional<Inspection> findById(int id) {
        String sql = """
            SELECT i.id, i.building_object_id, i.defect_id, i.inspection_date, i.inspector_name,
                   i.findings, i.measured_value, i.unit, i.notes, i.created_at,
                   b.name AS building_name
            FROM inspection i
            LEFT JOIN building_object b ON b.id = i.building_object_id
            WHERE i.id = ?
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
            log.error("findById Inspection id={}", id, e);
            throw new RuntimeException("Помилка пошуку обстеження", e);
        }
        return Optional.empty();
    }

    public Inspection create(Inspection insp) {
        String sql = """
            INSERT INTO inspection (building_object_id, defect_id, inspection_date, inspector_name,
                                    findings, measured_value, unit, notes)
            VALUES (?,?,?,?,?,?,?,?)
            """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, insp.getBuildingObjectId());
            if (insp.getDefectId() != null) ps.setInt(2, insp.getDefectId()); else ps.setNull(2, Types.INTEGER);
            ps.setDate(3, Date.valueOf(insp.getInspectionDate()));
            ps.setString(4, insp.getInspectorName());
            ps.setString(5, insp.getFindings());
            if (insp.getMeasuredValue() != null) ps.setBigDecimal(6, insp.getMeasuredValue()); else ps.setNull(6, Types.DECIMAL);
            ps.setString(7, insp.getUnit());
            ps.setString(8, insp.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    insp.setId(keys.getInt(1));
                }
            }
            log.info("Created Inspection id={}", insp.getId());
            return insp;
        } catch (SQLException e) {
            log.error("create Inspection failed", e);
            throw new RuntimeException("Помилка створення обстеження", e);
        }
    }

    public boolean update(Inspection insp) {
        String sql = """
            UPDATE inspection SET building_object_id=?, defect_id=?, inspection_date=?, inspector_name=?,
                                  findings=?, measured_value=?, unit=?, notes=?
            WHERE id=?
            """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, insp.getBuildingObjectId());
            if (insp.getDefectId() != null) ps.setInt(2, insp.getDefectId()); else ps.setNull(2, Types.INTEGER);
            ps.setDate(3, Date.valueOf(insp.getInspectionDate()));
            ps.setString(4, insp.getInspectorName());
            ps.setString(5, insp.getFindings());
            if (insp.getMeasuredValue() != null) ps.setBigDecimal(6, insp.getMeasuredValue()); else ps.setNull(6, Types.DECIMAL);
            ps.setString(7, insp.getUnit());
            ps.setString(8, insp.getNotes());
            ps.setInt(9, insp.getId());
            int updated = ps.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            log.error("update Inspection id={}", insp.getId(), e);
            throw new RuntimeException("Помилка оновлення обстеження", e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM inspection WHERE id=?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("delete Inspection id={}", id, e);
            throw new RuntimeException("Помилка видалення обстеження", e);
        }
    }

    public int createBatch(List<Inspection> items) {
        int count = 0;
        for (Inspection item : items) {
            create(item);
            count++;
        }
        return count;
    }

    private Inspection map(ResultSet rs) throws SQLException {
        Inspection i = new Inspection();
        i.setId(rs.getInt("id"));
        i.setBuildingObjectId(rs.getInt("building_object_id"));
        int defId = rs.getInt("defect_id");
        i.setDefectId(rs.wasNull() ? null : defId);
        Date d = rs.getDate("inspection_date");
        if (d != null) i.setInspectionDate(d.toLocalDate());
        i.setInspectorName(rs.getString("inspector_name"));
        i.setFindings(rs.getString("findings"));
        BigDecimal mv = rs.getBigDecimal("measured_value");
        i.setMeasuredValue(mv);
        i.setUnit(rs.getString("unit"));
        i.setNotes(rs.getString("notes"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) i.setCreatedAt(ts.toLocalDateTime());
        i.setBuildingObjectName(rs.getString("building_name"));
        return i;
    }
}
