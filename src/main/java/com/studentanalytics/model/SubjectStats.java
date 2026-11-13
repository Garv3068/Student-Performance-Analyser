package com.studentanalytics.model;

import java.io.Serializable;

/**
 * Class-wide statistics for one subject. All score values are percentages,
 * so subjects with different maximum marks can be compared fairly.
 */
public class SubjectStats implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String subjectName;
    private final int studentCount;
    private final double averagePercentage;
    private final double highestPercentage;
    private final String highestScorer;
    private final double lowestPercentage;
    private final String lowestScorer;
    private final int passCount;

    public SubjectStats(String subjectName, int studentCount, double averagePercentage,
                        double highestPercentage, String highestScorer,
                        double lowestPercentage, String lowestScorer, int passCount) {
        this.subjectName = subjectName;
        this.studentCount = studentCount;
        this.averagePercentage = averagePercentage;
        this.highestPercentage = highestPercentage;
        this.highestScorer = highestScorer;
        this.lowestPercentage = lowestPercentage;
        this.lowestScorer = lowestScorer;
        this.passCount = passCount;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public int getStudentCount() {
        return studentCount;
    }

    public double getAveragePercentage() {
        return averagePercentage;
    }

    public double getHighestPercentage() {
        return highestPercentage;
    }

    public String getHighestScorer() {
        return highestScorer;
    }

    public double getLowestPercentage() {
        return lowestPercentage;
    }

    public String getLowestScorer() {
        return lowestScorer;
    }

    public int getPassCount() {
        return passCount;
    }

    public int getFailCount() {
        return studentCount - passCount;
    }

    public double getPassRate() {
        return studentCount == 0 ? 0 : (passCount * 100.0) / studentCount;
    }

    @Override
    public String toString() {
        return String.format("%s: avg=%.2f%%, high=%.2f%% (%s), low=%.2f%% (%s), pass=%d/%d",
                subjectName, averagePercentage, highestPercentage, highestScorer,
                lowestPercentage, lowestScorer, passCount, studentCount);
    }
}