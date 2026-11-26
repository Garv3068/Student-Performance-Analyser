package com.studentanalytics.util;

import com.studentanalytics.model.Student;

import java.util.Comparator;

public final class StudentComparators {

    private StudentComparators() {

    }

    public static Comparator<Student> byPercentageDesc() {
        return new Comparator<Student>() {
            @Override
            public int compare(Student a, Student b) {
                return Double.compare(b.getPercentage(), a.getPercentage());
            }
        };
    }

    public static Comparator<Student> byTotalMarksDesc() {
        return new Comparator<Student>() {
            @Override
            public int compare(Student a, Student b) {
                return Double.compare(b.getTotalMarks(), a.getTotalMarks());
            }
        };
    }

    public static Comparator<Student> byNameAsc() {
        return new Comparator<Student>() {
            @Override
            public int compare(Student a, Student b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        };
    }

    public static Comparator<Student> byIdAsc() {
        return new Comparator<Student>() {
            @Override
            public int compare(Student a, Student b) {
                return a.getId().compareToIgnoreCase(b.getId());
            }
        };
    }

    public static Comparator<Student> forRanking() {
        return byPercentageDesc()
                .thenComparing(byTotalMarksDesc())
                .thenComparing(byNameAsc());
    }
}
