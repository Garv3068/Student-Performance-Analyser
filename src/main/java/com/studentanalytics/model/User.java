package com.studentanalytics.model;

/** A registered account. */
public class User {
    private final long id;
    private final String name;
    private final String email;
    private final String passwordHash;
    private final Role role;

    public User(long id, String name, String email, String passwordHash, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
}
