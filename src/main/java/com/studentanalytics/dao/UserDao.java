package com.studentanalytics.dao;

import com.studentanalytics.model.Role;
import com.studentanalytics.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** JDBC access to the users table. */
public class UserDao {

    private static final int MYSQL_DUPLICATE_KEY = 1062;

    /** Creates an account. Returns null when the email is already registered. */
    public User create(String name, String email, String passwordHash, Role role) {
        String sql = "INSERT INTO users (name, email, password_hash, role) VALUES (?, ?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setString(4, role.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new User(keys.getLong(1), name, email, passwordHash, role);
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == MYSQL_DUPLICATE_KEY) return null;
            throw new DataAccessException("Could not create the account.", e);
        }
    }

    /** Returns the user with this (lower-case) email, or null. */
    public User findByEmail(String email) {
        String sql = "SELECT id, name, email, password_hash, role FROM users WHERE email = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? new User(rs.getLong("id"), rs.getString("name"),
                                   rs.getString("email"), rs.getString("password_hash"),
                                   roleOf(rs.getString("role")))
                        : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not look up the account.", e);
        }
    }

    private static Role roleOf(String text) {
        Role r = Role.parse(text);
        return r == null ? Role.TEACHER : r;
    }
}