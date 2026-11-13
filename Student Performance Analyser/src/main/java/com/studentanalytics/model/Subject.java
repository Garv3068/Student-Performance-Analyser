package com.studentanalytics.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents one subject's result for a student.
 */
public class Subject implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final double DEFAULT_MAX_MARKS = 100.0;
    public static final double DEFAULT_PASS_PERCENTAGE = 40.0;

    private final String name;
    private final double marksObtained;
    private final double maxMarks;

    public Subject(String name, double marksObtained) {
        this(name, marksObtained, DEFAULT_MAX_MARKS);
    }

    public Subject(String name, double marksObtained, double maxMarks) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Subject name cannot be empty");
        }
        if (maxMarks <= 0) {
            throw new IllegalArgumentException("Maximum marks must be greater than 0");
        }
        if (marksObtained < 0 || marksObtained > maxMarks) {
            throw new IllegalArgumentException(
                    "Marks for " + name + " must be between 0 and " + maxMarks);
        }
        this.name = name.trim();
        this.marksObtained = marksObtained;
        this.maxMarks = maxMarks;
    }

    public String getName() {
        return name;
    }

    public double getMarksObtained() {
        return marksObtained;
    }

    public double getMaxMarks() {
        return maxMarks;
    }

    /** Percentage scored in this subject. */
    public double getPercentage() {
        return (marksObtained / maxMarks) * 100.0;
    }

    /** True if the percentage is at or above the pass mark (40% by default). */
    public boolean isPassed() {
        return getPercentage() >= DEFAULT_PASS_PERCENTAGE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Subject)) return false;
        Subject other = (Subject) o;
        return name.equalsIgnoreCase(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase());
    }

    @Override
    public String toString() {
        return name + ": " + marksObtained + "/" + maxMarks;
    }
}