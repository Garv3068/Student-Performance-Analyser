package com.studentanalytics.dao;

import com.studentanalytics.model.Student;
import com.studentanalytics.model.Subject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** MySQL implementation of {@link StudentDao}. Each call opens and closes its own connection. */
public class JdbcStudentDao implements StudentDao {

    private static final int DUPLICATE_KEY = 1062;
    private static final int NO_PARENT_ROW = 1452;

    private static final String INSERT_SUBJECT =
            "INSERT INTO subjects (user_id, student_id, name, marks, max_marks) VALUES (?, ?, ?, ?, ?)";

    @Override
    public List<Student> findAll(long userId) {
        try (Connection c = Database.getConnection()) {
            Map<String, Student> byKey = new LinkedHashMap<>();

            try (PreparedStatement ps = c.prepareStatement(SELECT_STUDENT
                    + " WHERE s.user_id = ? ORDER BY s.seq")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Student s = readStudent(rs);
                        byKey.put(key(s.getId()), s);
                    }
                }
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT student_id, name, marks, max_marks FROM subjects WHERE user_id = ? ORDER BY id")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Student owner = byKey.get(key(rs.getString("student_id")));
                        if (owner != null) {
                            owner.addSubject(readSubject(rs));
                        }
                    }
                }
            }
            return new ArrayList<>(byKey.values());
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    @Override
    public Student findById(long userId, String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        try (Connection c = Database.getConnection()) {
            Student student;
            try (PreparedStatement ps = c.prepareStatement(SELECT_STUDENT
                    + " WHERE s.user_id = ? AND s.id = ?")) {
                ps.setLong(1, userId);
                ps.setString(2, id.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    student = readStudent(rs);
                }
            }
            loadSubjects(c, student);
            return student;
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    @Override
    public Student findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        try (Connection c = Database.getConnection()) {
            Student student;
            try (PreparedStatement ps = c.prepareStatement(SELECT_STUDENT + " WHERE s.email = ?")) {
                ps.setString(1, email.trim().toLowerCase(Locale.ROOT));
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    student = readStudent(rs);
                }
            }
            loadSubjects(c, student);
            return student;
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    @Override
    public boolean emailRegistered(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM students WHERE email = ?")) {
            ps.setString(1, email.trim().toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    @Override
    public void insert(long userId, Student student) {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO students (user_id, id, name, email, class_id) VALUES (?, ?, ?, ?, ?)")) {
                    ps.setLong(1, userId);
                    ps.setString(2, student.getId());
                    ps.setString(3, student.getName());
                    ps.setString(4, student.getEmail());
                    if (student.getClassId() == null) {
                        ps.setNull(5, Types.BIGINT);
                    } else {
                        ps.setLong(5, student.getClassId());
                    }
                    ps.executeUpdate();
                } catch (SQLIntegrityConstraintViolationException e) {
                    throw duplicateStudent(e, student);
                }
                if (!student.getSubjects().isEmpty()) {
                    try (PreparedStatement ps = c.prepareStatement(INSERT_SUBJECT)) {
                        for (Subject s : student.getSubjects()) {
                            bindSubject(ps, userId, student.getId(), s);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    @Override
    public boolean delete(long userId, String id) {
        return update("DELETE FROM students WHERE user_id = ? AND id = ?", userId, id) > 0;
    }

    @Override
    public boolean updateName(long userId, String id, String name) {
        return update("UPDATE students SET name = ? WHERE user_id = ? AND id = ?", name, userId, id) > 0;
    }

    @Override
    public boolean updateDetails(long userId, String id, String name, String email, Long classId) {
        String sql = "UPDATE students SET name = ?, email = ?, class_id = ? "
                + "WHERE user_id = ? AND id = ? "
                + "AND (? IS NULL OR EXISTS (SELECT 1 FROM classes WHERE id = ? AND user_id = ?))";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            if (classId == null) {
                ps.setNull(3, Types.BIGINT);
                ps.setNull(6, Types.BIGINT);
                ps.setNull(7, Types.BIGINT);
            } else {
                ps.setLong(3, classId);
                ps.setLong(6, classId);
                ps.setLong(7, classId);
            }
            ps.setLong(4, userId);
            ps.setString(5, id);
            ps.setLong(8, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            if (e.getErrorCode() == DUPLICATE_KEY) {
                throw new IllegalArgumentException("Another student already uses the email " + email);
            }
            throw translate(e);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    @Override
    public boolean addSubject(long userId, String studentId, Subject subject) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(INSERT_SUBJECT)) {
            bindSubject(ps, userId, studentId, subject);
            ps.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            if (e.getErrorCode() == DUPLICATE_KEY) {
                throw new IllegalArgumentException("Subject already added: " + subject.getName());
            }
            if (e.getErrorCode() == NO_PARENT_ROW) {
                return false; // the student does not exist
            }
            throw translate(e);
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    @Override
    public boolean updateSubject(long userId, String studentId, Subject subject) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE subjects SET marks = ?, max_marks = ? WHERE user_id = ? AND student_id = ? AND name = ?")) {
            ps.setDouble(1, subject.getMarksObtained());
            ps.setDouble(2, subject.getMaxMarks());
            ps.setLong(3, userId);
            ps.setString(4, studentId);
            ps.setString(5, subject.getName());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    @Override
    public boolean deleteSubject(long userId, String studentId, String subjectName) {
        return update("DELETE FROM subjects WHERE user_id = ? AND student_id = ? AND name = ?",
                userId, studentId, subjectName == null ? null : subjectName.trim()) > 0;
    }

    // ---------- helpers ----------

    /** Runs one INSERT/UPDATE/DELETE with the given parameters and returns the row count. */
    private int update(String sql, Object... params) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    private static void bindSubject(PreparedStatement ps, long userId, String studentId, Subject s)
            throws SQLException {
        ps.setLong(1, userId);
        ps.setString(2, studentId);
        ps.setString(3, s.getName());
        ps.setDouble(4, s.getMarksObtained());
        ps.setDouble(5, s.getMaxMarks());
    }

    private static final String SELECT_STUDENT =
            "SELECT s.user_id, s.id, s.name, s.email, s.class_id, c.name AS class_name "
            + "FROM students s LEFT JOIN classes c ON c.id = s.class_id";

    private static Student readStudent(ResultSet rs) throws SQLException {
        Student s = new Student(rs.getString("id"), rs.getString("name"));
        s.setEmail(rs.getString("email"));
        long classId = rs.getLong("class_id");
        s.setSection(rs.wasNull() ? null : classId, rs.getString("class_name"));
        s.setOwnerId(rs.getLong("user_id"));
        return s;
    }

    private static void loadSubjects(Connection c, Student student) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT name, marks, max_marks FROM subjects WHERE user_id = ? AND student_id = ? ORDER BY id")) {
            ps.setLong(1, student.getOwnerId());
            ps.setString(2, student.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    student.addSubject(readSubject(rs));
                }
            }
        }
    }

    private static IllegalArgumentException duplicateStudent(SQLException e, Student student) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (msg.contains("uq_students_email")) {
            return new IllegalArgumentException("Another student already uses the email " + student.getEmail());
        }
        if (msg.contains("fk_students_class")) {
            return new IllegalArgumentException("That section no longer exists");
        }
        return new IllegalArgumentException("A student with ID " + student.getId() + " already exists");
    }

    private static Subject readSubject(ResultSet rs) throws SQLException {
        return new Subject(rs.getString("name"), rs.getDouble("marks"), rs.getDouble("max_marks"));
    }

    private static String key(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }

    private static RuntimeException translate(SQLException e) {
        if ("22001".equals(e.getSQLState())) {
            return new IllegalArgumentException("One of the values is too long");
        }
        return new DataAccessException("Database operation failed", e);
    }
}