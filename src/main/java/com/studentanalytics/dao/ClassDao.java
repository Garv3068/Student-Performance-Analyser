package com.studentanalytics.dao;

import com.studentanalytics.model.SchoolClass;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Sections / classes. Every method takes the teacher's user ID and only touches that
 * teacher's rows. Deleting a section keeps its students; they become "unassigned".
 */
public class ClassDao {

    private static final int DUPLICATE_KEY = 1062;

    /** All of the teacher's sections with their student counts, ordered by name. */
    public List<SchoolClass> findAll(long userId) {
        String sql = "SELECT c.id, c.name, COUNT(s.id) AS n FROM classes c "
                + "LEFT JOIN students s ON s.class_id = c.id "
                + "WHERE c.user_id = ? GROUP BY c.id, c.name ORDER BY c.name";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<SchoolClass> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new SchoolClass(rs.getLong("id"), rs.getString("name"), rs.getInt("n")));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the sections.", e);
        }
    }

    /** The section with this ID if it belongs to the teacher, else null. */
    public SchoolClass findById(long userId, long classId) {
        for (SchoolClass sc : findAll(userId)) {
            if (sc.getId() == classId) {
                return sc;
            }
        }
        return null;
    }

    /** Creates a section. Throws IllegalArgumentException if the name is already used. */
    public long create(long userId, String name) {
        String sql = "INSERT INTO classes (user_id, name) VALUES (?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setString(2, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw translate(e, name);
        }
    }

    /** Renames a section. Returns false if it does not exist. */
    public boolean rename(long userId, long classId, String name) {
        String sql = "UPDATE classes SET name = ? WHERE id = ? AND user_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setLong(2, classId);
            ps.setLong(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw translate(e, name);
        }
    }

    /** Deletes a section; its students stay and become unassigned. Returns false if it did not exist. */
    public boolean delete(long userId, long classId) {
        String sql = "DELETE FROM classes WHERE id = ? AND user_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, classId);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete the section.", e);
        }
    }

    private static RuntimeException translate(SQLException e, String name) {
        if (e.getErrorCode() == DUPLICATE_KEY) {
            return new IllegalArgumentException("You already have a section named " + name);
        }
        if ("22001".equals(e.getSQLState())) {
            return new IllegalArgumentException("The section name is too long");
        }
        return new DataAccessException("Database operation failed", e);
    }
}