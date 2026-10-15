package org.example.security;

/**
 * Defines operations for hashing and verifying passwords.
 */
public interface PasswordHasher {

    String hash(String password);

    boolean verify(String password, String hashedPassword);
}