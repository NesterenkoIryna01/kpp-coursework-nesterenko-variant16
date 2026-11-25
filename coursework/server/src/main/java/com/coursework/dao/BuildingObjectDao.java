package com.coursework.dao;

import com.coursework.model.BuildingObject;
import com.coursework.util.DbConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BuildingObjectDao {

    private static final Logger log = LoggerFactory.getLogger(BuildingObjectDao.class);

    public List<BuildingObject> findAll() {
        List<BuildingObject> list = new ArrayList<>();
        String sql = "SELECT id, name, address, object_type, year_built, floors, status, created_at FROM building_object ORDER BY id";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            log.error("findAll BuildingObject failed", e);
            throw new RuntimeException("Помилка отримання списку будівельних об’єктів", e);
        }
        return list;
    }

    public Optional<BuildingObject> findById(int id) {
        String sql = "SELECT id, name, address, object_type, year_built, floors, status, created_at FROM building_object WHERE id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            log.error("findById BuildingObject id={}", id, e);
            throw new RuntimeException("Помилка пошуку будівельного об’єкта", e);
        }
        return Optional.empty();
    }

    public BuildingObject create(BuildingObject obj) {
        String sql = "INSERT INTO building_object (name, address, object_type, year_built, floors, status) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, obj.getName());
            ps.setString(2, obj.getAddress());
            ps.setString(3, obj.getObjectType());
            if (obj.getYearBuilt() != null) ps.setInt(4, obj.getYearBuilt()); else ps.setNull(4, Types.INTEGER);
            if (obj.getFloors() != null) ps.setInt(5, obj.getFloors()); else ps.setNull(5, Types.INTEGER);
            ps.setString(6, obj.getStatus() != null ? obj.getStatus() : "ACTIVE");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    obj.setId(keys.getInt(1));
                }
            }
            log.info("Created BuildingObject id={}", obj.getId());
            return obj;
        } catch (SQLException e) {
            log.error("create BuildingObject failed", e);
            throw new RuntimeException("Помилка створення будівельного об’єкта", e);
        }
    }

    public boolean update(BuildingObject obj) {
        String sql = "UPDATE building_object SET name=?, address=?, object_type=?, year_built=?, floors=?, status=? WHERE id=?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, obj.getName());
            ps.setString(2, obj.getAddress());
            ps.setString(3, obj.getObjectType());
            if (obj.getYearBuilt() != null) ps.setInt(4, obj.getYearBuilt()); else ps.setNull(4, Types.INTEGER);
            if (obj.getFloors() != null) ps.setInt(5, obj.getFloors()); else ps.setNull(5, Types.INTEGER);
            ps.setString(6, obj.getStatus());
            ps.setInt(7, obj.getId());
            int updated = ps.executeUpdate();
            log.info("Updated BuildingObject id={}, rows={}", obj.getId(), updated);
            return updated > 0;
        } catch (SQLException e) {
            log.error("update BuildingObject id={}", obj.getId(), e);
            throw new RuntimeException("Помилка оновлення будівельного об’єкта", e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM building_object WHERE id=?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int deleted = ps.executeUpdate();
            log.info("Deleted BuildingObject id={}, rows={}", id, deleted);
            return deleted > 0;
        } catch (SQLException e) {
            log.error("delete BuildingObject id={}", id, e);
            throw new RuntimeException("Помилка видалення будівельного об’єкта", e);
        }
    }

    private BuildingObject map(ResultSet rs) throws SQLException {
        BuildingObject o = new BuildingObject();
        o.setId(rs.getInt("id"));
        o.setName(rs.getString("name"));
        o.setAddress(rs.getString("address"));
        o.setObjectType(rs.getString("object_type"));
        int yb = rs.getInt("year_built");
        o.setYearBuilt(rs.wasNull() ? null : yb);
        int fl = rs.getInt("floors");
        o.setFloors(rs.wasNull() ? null : fl);
        o.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) o.setCreatedAt(ts.toLocalDateTime());
        return o;
    }
}
