package com.coursework.dao;

import com.coursework.model.Defect;
import com.coursework.model.DefectState;
import com.coursework.util.DbConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DefectDao {

    private static final Logger log = LoggerFactory.getLogger(DefectDao.class);

    public List<Defect> findAll() {
        List<Defect> list = new ArrayList<>();
        String sql = """
            SELECT d.id, d.building_object_id, d.title, d.description, d.severity, d.area_scale,
                   d.safety_impact, d.integral_score, d.state, d.discovered_at, d.created_at, d.updated_at,
                   b.name AS building_name
            FROM defect d
            LEFT JOIN building_object b ON b.id = d.building_object_id
            ORDER BY d.id
            """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            log.error("findAll Defect failed", e);
            throw new RuntimeException("Помилка отримання списку дефектів", e);
        }
        return list;
    }

    public Optional<Defect> findById(int id) {
        String sql = """
            SELECT d.id, d.building_object_id, d.title, d.description, d.severity, d.area_scale,
                   d.safety_impact, d.integral_score, d.state, d.discovered_at, d.created_at, d.updated_at,
                   b.name AS building_name
            FROM defect d
            LEFT JOIN building_object b ON b.id = d.building_object_id
            WHERE d.id = ?
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
            log.error("findById Defect id={}", id, e);
            throw new RuntimeException("Помилка пошуку дефекту", e);
        }
        return Optional.empty();
    }

    public Defect create(Defect d) {
        String sql = """
            INSERT INTO defect (building_object_id, title, description, severity, area_scale, safety_impact, state, discovered_at)
            VALUES (?,?,?,?,?,?,?,?)
            """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParams(ps, d);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    d.setId(keys.getInt(1));
                }
            }
            // reload to get computed integral_score
            return findById(d.getId()).orElse(d);
        } catch (SQLException e) {
            log.error("create Defect failed", e);
            throw new RuntimeException("Помилка створення дефекту", e);
        }
    }

    public boolean update(Defect d) {
        String sql = """
            UPDATE defect SET building_object_id=?, title=?, description=?, severity=?, area_scale=?,
                              safety_impact=?, state=?, discovered_at=?
            WHERE id=?
            """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, d);
            ps.setInt(9, d.getId());
            int updated = ps.executeUpdate();
            log.info("Updated Defect id={}, rows={}", d.getId(), updated);
            return updated > 0;
        } catch (SQLException e) {
            log.error("update Defect id={}", d.getId(), e);
            throw new RuntimeException("Помилка оновлення дефекту", e);
        }
    }

    public boolean updateState(int id, DefectState newState) {
        String sql = "UPDATE defect SET state=? WHERE id=?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newState.name());
            ps.setInt(2, id);
            int updated = ps.executeUpdate();
            log.info("Updated Defect state id={} -> {}, rows={}", id, newState, updated);
            return updated > 0;
        } catch (SQLException e) {
            log.error("updateState Defect id={}", id, e);
            throw new RuntimeException("Помилка зміни стану дефекту", e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM defect WHERE id=?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int deleted = ps.executeUpdate();
            log.info("Deleted Defect id={}, rows={}", id, deleted);
            return deleted > 0;
        } catch (SQLException e) {
            log.error("delete Defect id={}", id, e);
            throw new RuntimeException("Помилка видалення дефекту", e);
        }
    }

    private void setParams(PreparedStatement ps, Defect d) throws SQLException {
        ps.setInt(1, d.getBuildingObjectId());
        ps.setString(2, d.getTitle());
        ps.setString(3, d.getDescription());
        ps.setBigDecimal(4, d.getSeverity() != null ? d.getSeverity() : BigDecimal.ZERO);
        ps.setBigDecimal(5, d.getAreaScale() != null ? d.getAreaScale() : BigDecimal.ZERO);
        ps.setBigDecimal(6, d.getSafetyImpact() != null ? d.getSafetyImpact() : BigDecimal.ZERO);
        ps.setString(7, d.getState() != null ? d.getState().name() : DefectState.ЗАФІКСОВАНО.name());
        if (d.getDiscoveredAt() != null) {
            ps.setDate(8, Date.valueOf(d.getDiscoveredAt()));
        } else {
            ps.setNull(8, Types.DATE);
        }
    }

    private Defect map(ResultSet rs) throws SQLException {
        Defect d = new Defect();
        d.setId(rs.getInt("id"));
        d.setBuildingObjectId(rs.getInt("building_object_id"));
        d.setTitle(rs.getString("title"));
        d.setDescription(rs.getString("description"));
        d.setSeverity(rs.getBigDecimal("severity"));
        d.setAreaScale(rs.getBigDecimal("area_scale"));
        d.setSafetyImpact(rs.getBigDecimal("safety_impact"));
        d.setIntegralScore(rs.getBigDecimal("integral_score"));
        d.setState(DefectState.fromString(rs.getString("state")));
        Date disc = rs.getDate("discovered_at");
        if (disc != null) d.setDiscoveredAt(disc.toLocalDate());
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) d.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) d.setUpdatedAt(updated.toLocalDateTime());
        d.setBuildingObjectName(rs.getString("building_name"));
        return d;
    }
}
