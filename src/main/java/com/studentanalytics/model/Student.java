package com.studentanalytics.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Student implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final List<Subject> subjects = new ArrayList<>();
    private String grade;
    private int rank;

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
