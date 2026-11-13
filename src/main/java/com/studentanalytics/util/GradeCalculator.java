package com.studentanalytics.util;

import com.studentanalytics.model.Student;
import com.studentanalytics.model.Subject;

import java.util.Collection;


public class GradeCalculator {

    public String calculateGrade(double percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException(
                    "Percentage must be between 0 and 100, got " + percentage);
        }
        if (percentage > 80) return "O";
        if (percentage > 70) return "A+";
        if (percentage > 60) return "A";
        if (percentage >= 50) return "B+";
        if (percentage >= 40) return "B";
        if (percentage >= 30) return "C";
        return "D";
    }

    public String calculateGrade(Subject subject) {
        return calculateGrade(subject.getPercentage());
    }

    public String calculateGrade(Student student) {
        for (Subject s : student.getSubjects()) {
            if (!s.isPassed()) {
                return "F";
            }
        }
        return calculateGrade(student.getPercentage());
    }

    public String assignGrade(Student student) {
        String grade = calculateGrade(student);
        student.setGrade(grade);
        return grade;
    }

    public void assignGrades(Collection<Student> students) {
        for (Student student : students) {
            assignGrade(student);
        }
    }

    public boolean isPass(String grade) {
        return grade != null && !grade.equals("F");
    }
}