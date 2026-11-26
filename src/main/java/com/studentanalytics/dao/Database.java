package com.studentanalytics.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Opens MySQL connections and creates the tables on first run.
 *
 * Settings are read, in this order, from the environment variables DB_URL, DB_USER
 * and DB_PASSWORD, then from db.properties on the classpath, then the defaults below.
 */
public final class Database {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/student_analytics"
            + "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    // Every student belongs to one user. A student ID only has to be unique within that
    // user, so two users can both have a student "101" without seeing each other's data.
    // A section / class belongs to one teacher. Section names are unique per teacher.
    private static final String CREATE_CLASSES =
            "CREATE TABLE IF NOT EXISTS classes ("
            + " id BIGINT NOT NULL AUTO_INCREMENT,"
            + " user_id BIGINT NOT NULL,"
            + " name VARCHAR(40) NOT NULL,"
            + " created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + " PRIMARY KEY (id),"
            + " UNIQUE KEY uq_classes_name (user_id, name),"
            + " CONSTRAINT fk_classes_owner FOREIGN KEY (user_id)"
            + "   REFERENCES users (id) ON DELETE CASCADE"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    private static final String CREATE_STUDENTS =
            "CREATE TABLE IF NOT EXISTS students ("
            + " seq BIGINT NOT NULL AUTO_INCREMENT,"
            + " user_id BIGINT NOT NULL,"
            + " id VARCHAR(64) NOT NULL,"
            + " name VARCHAR(100) NOT NULL,"
            + " email VARCHAR(254) NULL,"
            + " class_id BIGINT NULL,"
            + " created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + " PRIMARY KEY (user_id, id),"
            + " UNIQUE KEY uq_students_seq (seq),"
            + " UNIQUE KEY uq_students_email (email),"
            + " CONSTRAINT fk_students_owner FOREIGN KEY (user_id)"
            + "   REFERENCES users (id) ON DELETE CASCADE,"
            + " CONSTRAINT fk_students_class FOREIGN KEY (class_id)"
            + "   REFERENCES classes (id) ON DELETE SET NULL"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    private static final String CREATE_SUBJECTS =
            "CREATE TABLE IF NOT EXISTS subjects ("
            + " id BIGINT NOT NULL AUTO_INCREMENT,"
            + " user_id BIGINT NOT NULL,"
            + " student_id VARCHAR(64) NOT NULL,"
            + " name VARCHAR(40) NOT NULL,"
            + " marks DOUBLE NOT NULL,"
            + " max_marks DOUBLE NOT NULL,"
            + " PRIMARY KEY (id),"
            + " UNIQUE KEY uq_student_subject (user_id, student_id, name),"
            + " CONSTRAINT fk_subjects_student_owner FOREIGN KEY (user_id, student_id)"
            + "   REFERENCES students (user_id, id) ON DELETE CASCADE ON UPDATE CASCADE,"
            + " CONSTRAINT chk_subjects_marks_v2"
            + "   CHECK (marks >= 0 AND max_marks > 0 AND marks <= max_marks)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    private static final String CREATE_USERS =
            "CREATE TABLE IF NOT EXISTS users ("
            + " id BIGINT NOT NULL AUTO_INCREMENT,"
            + " name VARCHAR(100) NOT NULL,"
            + " email VARCHAR(254) NOT NULL,"
            + " password_hash VARCHAR(255) NOT NULL,"
            + " role VARCHAR(10) NOT NULL DEFAULT 'TEACHER',"
            + " created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + " PRIMARY KEY (id),"
            + " UNIQUE KEY uq_users_email (email)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    private static final Properties FILE_CONFIG = loadFileConfig();

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
            // DriverManager will report "No suitable driver" if the jar is really missing.
        }
    }

    private Database() {
        // utility class
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                setting("DB_URL", "db.url", DEFAULT_URL),
                setting("DB_USER", "db.user", DEFAULT_USER),
                setting("DB_PASSWORD", "db.password", DEFAULT_PASSWORD));
    }

    /**
     * Creates the tables if they do not exist yet. Safe to call on every start.
     *
     * If the database still has the old shared tables (students without a user_id column),
     * they are renamed to students_legacy_global / subjects_legacy_global, not deleted,
     * and new per-user tables are created.
     */
    public static synchronized void initSchema() {
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate(CREATE_USERS);
            // Accounts made before roles existed were all teachers (each owned a workspace).
            if (!columnExists(c, "users", "role")) {
                st.executeUpdate("ALTER TABLE users ADD COLUMN role VARCHAR(10) NOT NULL DEFAULT 'TEACHER'");
            }
            st.executeUpdate(CREATE_CLASSES);
            if (tableExists(c, "students") && !columnExists(c, "students", "user_id")) {
                if (tableExists(c, "subjects")) {
                    st.executeUpdate("RENAME TABLE subjects TO subjects_legacy_global");
                }
                st.executeUpdate("RENAME TABLE students TO students_legacy_global");
            }
            st.executeUpdate(CREATE_STUDENTS);
            // Upgrade tables created by an earlier version of the app.
            if (!columnExists(c, "students", "email")) {
                st.executeUpdate("ALTER TABLE students ADD COLUMN email VARCHAR(254) NULL, "
                        + "ADD UNIQUE KEY uq_students_email (email)");
            }
            if (!columnExists(c, "students", "class_id")) {
                st.executeUpdate("ALTER TABLE students ADD COLUMN class_id BIGINT NULL, "
                        + "ADD CONSTRAINT fk_students_class FOREIGN KEY (class_id) "
                        + "REFERENCES classes (id) ON DELETE SET NULL");
            }
            st.executeUpdate(CREATE_SUBJECTS);
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not connect to MySQL or create the tables. Check db.properties "
                    + "(URL, user, password) and that MySQL is running.", e);
        }
    }

    private static boolean tableExists(Connection c, String table) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean columnExists(Connection c, String table, String column) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = ? AND column_name = ?")) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static String setting(String envName, String propName, String fallback) {
        String fromEnv = System.getenv(envName);
        if (fromEnv != null && !fromEnv.isEmpty()) {
            return fromEnv;
        }
        return FILE_CONFIG.getProperty(propName, fallback);
    }

    private static Properties loadFileConfig() {
        Properties props = new Properties();
        try (InputStream in = Database.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            // fall back to the defaults
        }
        return props;
    }
}