package com.studentanalytics.model;

import java.io.Serializable;

/** A section / class created by a teacher, e.g. "10-A". */
public class SchoolClass implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long id;
    private final String name;
    private final int studentCount;

    public SchoolClass(long id, String name, int studentCount) {
        this.id = id;
        this.name = name;
        this.studentCount = studentCount;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public int getStudentCount() { return studentCount; }
}
