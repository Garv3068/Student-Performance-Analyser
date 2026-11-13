package com.studentanalytics.service;

import com.studentanalytics.model.Student;
import com.studentanalytics.model.Subject;
import com.studentanalytics.model.SubjectStats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Performs the analytics for the tool:
 *  - highest / lowest scoring subject per student (requirement 3)
 *  - class average and subject-wise statistics (requirement 5)
 */
public class AnalyticsService {

    // ---------- Per-student analytics (requirement 3) ----------

    /** Subject with the student's highest percentage, or null if no subjects. */
    public Subject getHighestSubject(Student student) {
        return student.getHighestSubject();
    }

    /** Subject with the student's lowest percentage, or null if no subjects. */
    public Subject getLowestSubject(Student student) {
        return student.getLowestSubject();
    }

    // ---------- Class-level analytics (requirement 5) ----------

    /** Average of all students' overall percentages (0 if no students). */
    public double calculateClassAverage(Collection<Student> students) {
        if (students == null || students.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (Student s : students) {
            sum += s.getPercentage();
        }
        return sum / students.size();
    }

    /** Student with the highest overall percentage, or null if none. */
    public Student findTopper(Collection<Student> students) {
        if (students == null) return null;
        return students.stream()
                .max(Comparator.comparingDouble(Student::getPercentage))
                .orElse(null);
    }

    /** Student with the lowest overall percentage, or null if none. */
    public Student findLowestScorer(Collection<Student> students) {
        if (students == null) return null;
        return students.stream()
                .min(Comparator.comparingDouble(Student::getPercentage))
                .orElse(null);
    }

    /** Percentage of students who passed every subject (0 if no students). */
    public double calculatePassPercentage(Collection<Student> students) {
        if (students == null || students.isEmpty()) {
            return 0;
        }
        int passed = 0;
        for (Student s : students) {
            if (hasPassedAll(s)) {
                passed++;
            }
        }
        return (passed * 100.0) / students.size();
    }

    /**
     * Builds statistics for each subject across all students.
     * Subject names are matched case-insensitively. The returned map keeps
     * subjects in the order they were first encountered.
     */
    public Map<String, SubjectStats> calculateSubjectStats(Collection<Student> students) {
        Map<String, Accumulator> accumulators = new LinkedHashMap<>();

        if (students != null) {
            for (Student student : students) {
                for (Subject subject : student.getSubjects()) {
                    String key = subject.getName().toLowerCase();
                    accumulators
                            .computeIfAbsent(key, k -> new Accumulator(subject.getName()))
                            .add(student, subject);
                }
            }
        }

        Map<String, SubjectStats> result = new LinkedHashMap<>();
        for (Accumulator acc : accumulators.values()) {
            result.put(acc.name, acc.toStats());
        }
        return result;
    }

    /** Subject with the highest class average, or null if there is no data. */
    public SubjectStats findBestSubject(Collection<Student> students) {
        return calculateSubjectStats(students).values().stream()
                .max(Comparator.comparingDouble(SubjectStats::getAveragePercentage))
                .orElse(null);
    }

    /** Subject with the lowest class average, or null if there is no data. */
    public SubjectStats findWeakestSubject(Collection<Student> students) {
        return calculateSubjectStats(students).values().stream()
                .min(Comparator.comparingDouble(SubjectStats::getAveragePercentage))
                .orElse(null);
    }

    /** Convenience method: subject stats as a list, for use in a JSP loop. */
    public List<SubjectStats> getSubjectStatsList(Collection<Student> students) {
        return new ArrayList<>(calculateSubjectStats(students).values());
    }

    // ---------- Helpers ----------

    private boolean hasPassedAll(Student student) {
        for (Subject s : student.getSubjects()) {
            if (!s.isPassed()) {
                return false;
            }
        }
        return true;
    }

    /** Collects running totals for one subject while looping over students. */
    private static class Accumulator {
        private final String name;
        private int count;
        private double sumPercentage;
        private double highest = Double.NEGATIVE_INFINITY;
        private String highestScorer;
        private double lowest = Double.POSITIVE_INFINITY;
        private String lowestScorer;
        private int passCount;

        Accumulator(String name) {
            this.name = name;
        }

        void add(Student student, Subject subject) {
            double pct = subject.getPercentage();
            count++;
            sumPercentage += pct;
            if (pct > highest) {
                highest = pct;
                highestScorer = student.getName();
            }
            if (pct < lowest) {
                lowest = pct;
                lowestScorer = student.getName();
            }
            if (subject.isPassed()) {
                passCount++;
            }
        }

        SubjectStats toStats() {
            return new SubjectStats(name, count, sumPercentage / count,
                    highest, highestScorer, lowest, lowestScorer, passCount);
        }
    }
}