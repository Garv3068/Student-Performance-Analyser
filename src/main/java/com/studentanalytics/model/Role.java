package com.studentanalytics.model;

/**
 * The two kinds of account. The role decides which part of the site a person may open:
 * TEACHER manages sections and students, STUDENT can only view their own record.
 */
public enum Role {
    TEACHER("classes"),
    STUDENT("portal");

    private final String home;

    Role(String home) {
        this.home = home;
    }

    /** Page (relative to the app root) a person with this role lands on after signing in. */
    public String getHome() {
        return home;
    }

    /** Parses "teacher" / "STUDENT" etc. Returns null for anything else. */
    public static Role parse(String text) {
        if (text == null) {
            return null;
        }
        for (Role r : values()) {
            if (r.name().equalsIgnoreCase(text.trim())) {
                return r;
            }
        }
        return null;
    }
}
