package com.services;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * AuthenticationService
 * ---------------------------------------------
 * Lightweight PBKDF2 password hashing utility that does NOT
 * depend on Spring Security (so it compiles with plain JDK).
 *
 * Format stored in DB:  salt:iterations:hash
 */
public class AuthenticationService {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LENGTH = 16;     // bytes
    private static final int HASH_LENGTH = 32;     // bytes (256-bit)
    private static final int ITERATIONS = 100_000; // good compromise for demo

    private final SecureRandom secureRandom = new SecureRandom();

    public String hashPassword(String password) {
        if (password == null) {
            return null;
        }
        try {
            byte[] salt = new byte[SALT_LENGTH];
            secureRandom.nextBytes(salt);

            byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, HASH_LENGTH);

            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);

            // Stored format: salt:iterations:hash
            return saltB64 + ":" + ITERATIONS + ":" + hashB64;
        } catch (Exception e) {
            // In case of any failure, return null (registration should fail gracefully)
            System.err.println("[AuthenticationService] hashPassword error: " + e.getMessage());
            return null;
        }
    }

    public boolean verifyPassword(String raw, String encoded) {
        if (raw == null || encoded == null) {
            return false;
        }
        try {
            String[] parts = encoded.split(":");
            if (parts.length != 3) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            int iterations = Integer.parseInt(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

            byte[] actualHash = pbkdf2(raw.toCharArray(), salt, iterations, expectedHash.length);

            return slowEquals(expectedHash, actualHash);
        } catch (Exception e) {
            System.err.println("[AuthenticationService] verifyPassword error: " + e.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int length)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, length * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
        return skf.generateSecret(spec).getEncoded();
    }

    /** Constant‑time comparison to avoid timing attacks. */
    private boolean slowEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}