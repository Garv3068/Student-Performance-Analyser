package com.studentanalytics.service;

import com.studentanalytics.model.Student;
import com.studentanalytics.util.StudentComparators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Sorts students and assigns ranks (requirement 4).
 *
 * Ordering follows StudentComparators.forRanking():
 *  1. higher percentage first
 *  2. if tied, higher total marks first
 *  3. if still tied, name A to Z
 *
 * Ties use standard competition ranking ("1224"): students with the same
 * percentage AND the same total marks share a rank, and the next rank is
 * skipped. For example: 1, 2, 2, 4. Name is only used to order tied students
 * in the list; it never separates their ranks.
 */
public class RankingService {

    /** Differences smaller than this are treated as equal for doubles. */
    private static final double EPSILON = 1e-9;

    /**
     * Sorts the students in ranking order and sets each student's rank.
     * The collection passed in is not modified; a new sorted list is returned.
     * Returns an empty list if students is null or empty.
     */
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

    /**
     * Returns the top N students (ranks are assigned first). Because of ties,
     * the list may be longer than n if students share the rank at the cut-off
     * position; use getTopN(students, n, false) for a strict limit.
     */
    public List<Student> getTopN(Collection<Student> students, int n) {
        return getTopN(students, n, true);
    }

    /**
     * Returns the top N students.
     *
     * @param includeTies if true, everyone sharing the rank at position N is
     *                    included; if false, the list is cut at exactly N.
     */
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

    /** All students holding rank 1 (more than one if there is a tie). */
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

    /**
     * Finds a student's rank by ID (case-insensitive).
     *
     * @return the rank, or -1 if no student with that ID is in the collection
     */
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

    /** Returns an unmodifiable view, handy for passing a ranked list to a JSP. */
    public List<Student> rankStudentsReadOnly(Collection<Student> students) {
        return Collections.unmodifiableList(rankStudents(students));
    }

    // ---------- Helpers ----------

    /** Two students are tied if percentage and total marks are both equal. */
    private boolean isTied(Student a, Student b) {
        return Math.abs(a.getPercentage() - b.getPercentage()) < EPSILON
                && Math.abs(a.getTotalMarks() - b.getTotalMarks()) < EPSILON;
    }
}