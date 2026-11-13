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

public class AnalyticsService {


    public Subject getHighestSubject(Student student) {
        return student.getHighestSubject();
    }

    public Subject getLowestSubject(Student student) {
        return student.getLowestSubject();
    }

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

    public Student findTopper(Collection<Student> students) {
        if (students == null) return null;
        return students.stream()
                .max(Comparator.comparingDouble(Student::getPercentage))
                .orElse(null);
    }

    public Student findLowestScorer(Collection<Student> students) {
        if (students == null) return null;
        return students.stream()
                .min(Comparator.comparingDouble(Student::getPercentage))
                .orElse(null);
    }

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

    public SubjectStats findBestSubject(Collection<Student> students) {
        return calculateSubjectStats(students).values().stream()
                .max(Comparator.comparingDouble(SubjectStats::getAveragePercentage))
                .orElse(null);
    }

    public SubjectStats findWeakestSubject(Collection<Student> students) {
        return calculateSubjectStats(students).values().stream()
                .min(Comparator.comparingDouble(SubjectStats::getAveragePercentage))
                .orElse(null);
    }

    public List<SubjectStats> getSubjectStatsList(Collection<Student> students) {
        return new ArrayList<>(calculateSubjectStats(students).values());
    }


    private boolean hasPassedAll(Student student) {
        for (Subject s : student.getSubjects()) {
            if (!s.isPassed()) {
                return false;
            }
        }
        return true;
    }

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