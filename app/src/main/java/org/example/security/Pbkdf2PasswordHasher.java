package org.example.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Implements password hashing and verification using PBKDF2 with HMAC-SHA256.
 *
 * <p>Each password is hashed with a randomly generated salt. The resulting
 * string contains the algorithm identifier, iteration count, salt, and
 * derived hash.
 */
public final class Pbkdf2PasswordHasher implements PasswordHasher {

    private static final int ITERATIONS = 600_000;

    private final SecureRandom random = new SecureRandom();

    /**
     * Generates a salted PBKDF2 hash for the given password.
     *
     * @param password the plaintext password
     * @return the encoded password hash
     * @throws IllegalArgumentException if the password is null or empty
     */
    @Override
    public String hash(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        byte[] salt = new byte[16];
        random.nextBytes(salt);

        return "pbkdf2-sha256$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(
                derive(password, salt, ITERATIONS));
    }

    /**
     * Verifies a password against a previously generated PBKDF2 hash.
     *
     * @param password the plaintext password to verify
     * @param hashedPassword the encoded password hash
     * @return true if the password matches; false otherwise
     */
    @Override
    public boolean verify(String password, String hashedPassword) {
        if (password == null || hashedPassword == null) {
            return false;
        }

        try {
            String[] parts = hashedPassword.split("\\$", -1);

            if (parts.length != 4 || !parts[0].equals("pbkdf2-sha256")) {
                return false;
            }

            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);

            if (iterations < ITERATIONS || iterations > 1_200_000
                    || salt.length != 16 || expected.length != 32) {
                return false;
            }

            return MessageDigest.isEqual(
                    expected, derive(password, salt, iterations));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Derives a 256-bit key using PBKDF2 with HMAC-SHA256.
     *
     * @param password the plaintext password
     * @param salt the salt used for key derivation
     * @param iterations the number of PBKDF2 iterations
     * @return the derived key as a byte array
     * @throws IllegalStateException if the PBKDF2 algorithm is unavailable
     */
    private byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(), salt, iterations, 256);

        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }
}