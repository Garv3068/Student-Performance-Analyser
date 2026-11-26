package com.studentanalytics.dao;

import com.studentanalytics.model.Student;
import com.studentanalytics.model.Subject;

import java.util.List;

/**
 * Storage operations for students and their subjects.
 *
 * Every method takes the ID of the signed-in user and only ever sees that user's rows,
 * so each user has a private workspace.
 *
 * Methods that find nothing return null or false. Rule violations the user can fix
 * (duplicate ID, duplicate subject, value too long) throw IllegalArgumentException;
 * real database failures throw {@link DataAccessException}.
 */
public interface StudentDao {

    /** All students with their subjects, in the order they were added. */
    List<Student> findAll(long userId);

    /** One student with subjects, or null. The ID match ignores case. */
    Student findById(long userId, String id);

    /** Saves a student and all of their subjects in one transaction. */
    void insert(long userId, Student student);

    /** Deletes a student and their subjects. Returns false if there was no such student. */
    boolean delete(long userId, String id);

    boolean updateName(long userId, String id, String name);

    /**
     * Changes a student's name, email and section in one go. {@code classId} may be null
     * (no section) and must belong to the same teacher. Returns false if the student does not exist.
     * Throws IllegalArgumentException if the email is already used by another student.
     */
    boolean updateDetails(long userId, String id, String name, String email, Long classId);

    /** The student record registered under this email (any teacher), or null. Used by the student portal. */
    Student findByEmail(String email);

    /** True if some student record carries this email. Used to let a student sign up. */
    boolean emailRegistered(String email);

    /** Returns false if the student does not exist. */
    boolean addSubject(long userId, String studentId, Subject subject);

    boolean updateSubject(long userId, String studentId, Subject subject);

    boolean deleteSubject(long userId, String studentId, String subjectName);
}