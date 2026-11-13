package com.studentanalytics.service;

import com.studentanalytics.model.Student;
import com.studentanalytics.util.StudentComparators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class RankingService {

    private static final double EPSILON = 1e-9;

    public List<Student> rankStudents(Collection<Student> students) {
        if (students == null || students.isEmpty()) {
            return new ArrayList<>();
        }

        List<Student> sorted = new ArrayList<>(students);
        sorted.sort(StudentComparators.forRanking());

        int rank = 1;
        for (int i = 0; i < sorted.size(); i++) {
            Student current = sorted.get(i);
            if (i > 0 && !isTied(sorted.get(i - 1), current)) {
                rank = i + 1; // skip ranks consumed by the tied group
            }
            current.setRank(rank);
        }
        return sorted;
    }

    public List<Student> getTopN(Collection<Student> students, int n) {
        return getTopN(students, n, true);
    }

    public List<Student> getTopN(Collection<Student> students, int n, boolean includeTies) {
        if (n <= 0) {
            return new ArrayList<>();
        }
        List<Student> ranked = rankStudents(students);
        if (ranked.size() <= n) {
            return ranked;
        }
        int end = n;
        if (includeTies) {
            int cutoffRank = ranked.get(n - 1).getRank();
            while (end < ranked.size() && ranked.get(end).getRank() == cutoffRank) {
                end++;
            }
        }
        return new ArrayList<>(ranked.subList(0, end));
    }

    public List<Student> getToppers(Collection<Student> students) {
        List<Student> ranked = rankStudents(students);
        List<Student> toppers = new ArrayList<>();
        for (Student s : ranked) {
            if (s.getRank() != 1) {
                break;
            }
            toppers.add(s);
        }
        return toppers;
    }

    public int getRankById(Collection<Student> students, String studentId) {
        if (studentId == null) {
            return -1;
        }
        for (Student s : rankStudents(students)) {
            if (s.getId().equalsIgnoreCase(studentId.trim())) {
                return s.getRank();
            }
        }
        return -1;
    }

    public List<Student> rankStudentsReadOnly(Collection<Student> students) {
        return Collections.unmodifiableList(rankStudents(students));
    }

    private boolean isTied(Student a, Student b) {
        return Math.abs(a.getPercentage() - b.getPercentage()) < EPSILON
                && Math.abs(a.getTotalMarks() - b.getTotalMarks()) < EPSILON;
    }
}