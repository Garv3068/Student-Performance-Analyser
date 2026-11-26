package com.studentanalytics.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Student implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private String name;
    private final List<Subject> subjects = new ArrayList<>();
    private String grade;
    private int rank;
    private String email;        // optional; a student signs up with this email to see their own marks
    private Long classId;        // section the student belongs to, or null
    private String className;
    private long ownerId;        // id of the teacher account that owns this record

    public Student(String id, String name) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Student name cannot be empty");
        }
        this.id = id.trim();
        this.name = name.trim();
    }

    private static final java.util.regex.Pattern EMAIL =
            java.util.regex.Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");

    /** Sets the contact / login email. Blank clears it; anything else must look like an email. */
    public void setEmail(String newEmail) {
        if (newEmail == null || newEmail.trim().isEmpty()) {
            this.email = null;
            return;
        }
        String e = newEmail.trim().toLowerCase(java.util.Locale.ROOT);
        if (e.length() > 254 || !EMAIL.matcher(e).matches()) {
            throw new IllegalArgumentException("Enter a valid email address or leave it blank");
        }
        this.email = e;
    }

    public String getEmail() { return email; }

    public Long getClassId() { return classId; }
    public String getClassName() { return className; }

    public void setSection(Long classId, String className) {
        this.classId = classId;
        this.className = className;
    }

    public long getOwnerId() { return ownerId; }
    public void setOwnerId(long ownerId) { this.ownerId = ownerId; }

    public void addSubject(Subject subject) {
        if (subject == null) {
            throw new IllegalArgumentException("Subject cannot be null");
        }
        if (subjects.contains(subject)) {
            throw new IllegalArgumentException(
                    "Subject already added: " + subject.getName());
        }
        subjects.add(subject);
    }

    public void setName(String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Student name cannot be empty");
        }
        this.name = newName.trim();
    }

    public void replaceSubject(Subject updated) {
        if (updated == null) {
            throw new IllegalArgumentException("Subject cannot be null");
        }
        int index = subjects.indexOf(updated);
        if (index < 0) {
            throw new IllegalArgumentException("No subject named " + updated.getName());
        }
        subjects.set(index, updated);
    }

    /** Removes the subject with this name; returns false if there was none. */
    public boolean removeSubject(String subjectName) {
        if (subjectName == null) {
            return false;
        }
        String wanted = subjectName.trim();
        return subjects.removeIf(s -> s.getName().equalsIgnoreCase(wanted));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Subject> getSubjects() {
        return Collections.unmodifiableList(subjects);
    }

    public double getTotalMarks() {
        double total = 0;
        for (Subject s : subjects) {
            total += s.getMarksObtained();
        }
        return total;
    }

    public double getMaxTotalMarks() {
        double max = 0;
        for (Subject s : subjects) {
            max += s.getMaxMarks();
        }
        return max;
    }

    public double getPercentage() {
        double max = getMaxTotalMarks();
        return max == 0 ? 0 : (getTotalMarks() / max) * 100.0;
    }

    public Subject getHighestSubject() {
        return subjects.stream()
                .max(Comparator.comparingDouble(Subject::getPercentage))
                .orElse(null);
    }

    public Subject getLowestSubject() {
        return subjects.stream()
                .min(Comparator.comparingDouble(Subject::getPercentage))
                .orElse(null);
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    @Override
    public String toString() {
        return String.format("Student[%s, %s, %.2f%%, grade=%s]",
                id, name, getPercentage(), grade);
    }
}
