package com.studentanalytics.util;

import com.studentanalytics.model.Student;

import java.util.Comparator;

/**
 * Ready-made Comparators for sorting and ranking students (requirement 4).
 * Use with Collections.sort(list, StudentComparators.forRanking())
 * or list.sort(StudentComparators.forRanking()).
 */
public final class StudentComparators {

    private StudentComparators() {
        // utility class, no instances
    }

    /** Highest overall percentage first. */
    public static Comparator<Student> byPercentageDesc() {
        return new Comparator<Student>() {
            @Override
            public int compare(Student a, Student b) {
                return Double.compare(b.getPercentage(), a.getPercentage());
            }
        };
    }

    /** Highest total marks first. */
    public static Comparator<Student> byTotalMarksDesc() {
        return new Comparator<Student>() {
            @Override
            public int compare(Student a, Student b) {
                return Double.compare(b.getTotalMarks(), a.getTotalMarks());
            }
        };
    }

    /** Alphabetical by name, ignoring case. */
    public static Comparator<Student> byNameAsc() {
        return new Comparator<Student>() {
            @Override
            public int compare(Student a, Student b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        };
    }

    /** Ascending by student ID. */
    public static Comparator<Student> byIdAsc() {
        return new Comparator<Student>() {
            @Override
            public int compare(Student a, Student b) {
                return a.getId().compareToIgnoreCase(b.getId());
            }
        };
    }

    /**
     * Default ranking order:
     * 1. higher percentage first
     * 2. if tied, higher total marks first
     * 3. if still tied, name A to Z
     */
    public static Comparator<Student> forRanking() {
        return byPercentageDesc()
                .thenComparing(byTotalMarksDesc())
                .thenComparing(byNameAsc());
    }
}