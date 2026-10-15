package org.example.model;

public class User {

    private final String idKey;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final String email;
    private final String phone;
    private final UserRole role;

    public User(
            String idKey,
            String username,
            String passwordHash,
            String fullName,
            String email,
            String phone,
            UserRole role) {

        this.idKey = idKey;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
    }

    public String getIdKey() {
        return idKey;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public UserRole getRole() {
        return role;
    }
}