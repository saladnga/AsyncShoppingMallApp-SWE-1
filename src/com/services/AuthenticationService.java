package com.services;

import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import java.sql.*;
import java.util.Optional;
import com.common.dto.RegistrationRequest;

public class AuthenticationService {
    private static final String url = "jdbc:sqlite:shopping_mall.db";
    private static final Pbkdf2PasswordEncoder encoder = new Pbkdf2PasswordEncoder(
            "", // secret
            16, // salt length (bytes)
            310000, // iterations (Spring recommended default)
            Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);

    public String hashPassword(String password) {
        if (password == null)
            return null;
        return encoder.encode(password);
    }

    public boolean verifyPassword(String raw, String encoded) {
        if (raw == null || encoded == null)
            return false;
        return encoder.matches(raw, encoded);
    }

    public Optional<String> validateRegistration(RegistrationRequest request) {
        if (request == null)
            return Optional.of("Invalid Registration request");

        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        String email = request.getEmail() != null ? request.getEmail().trim() : "";
        String password = request.getPassword() != null ? request.getPassword() : "";

        if (username.isEmpty())
            return Optional.of("Username is required");
        if (email.isEmpty())
            return Optional.of("Email is required");
        if (password.isEmpty())
            return Optional.of("Password is required");

        if (username.length() < 3 || username.length() > 30)
            return Optional.of("User name must be between 3 and 30 characters");

        if (!username.matches("[A-Za-z0-9._-]{3,30}$"))
            return Optional.of("Username contains invalid characters (allowed: letters, numbers, ., _, -)");

        if (!email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
            return Optional.of("Email format is invalid");

        if (password.length() < 8)
            return Optional.of("Password must be at least 8 characters");

        if (!password.matches(".*[a-z].*"))
            return Optional.of("Password must include a lowercase letter");

        if (!password.matches(".*[A-Z].*"))
            return Optional.of("Password must include an uppercase letter");

        if (!password.matches(".*\\d.*"))
            return Optional.of("Password must include a digit");

        if (!password.matches(".*[^A-Za-z0-9].*"))
            return Optional.of("Password must include a special character");

        String sql = "SELECT 1 FROM users WHERE username = ? OR email = ? LIMIT 1";
        try (Connection connection = DriverManager.getConnection(url)) {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, username);
            statement.setString(2, email);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next())
                    return Optional.of("Username or email already in use");
            }
        } catch (SQLException ex) {
            return Optional.of("Unable to validate registration (Database Error)");
        }

        return Optional.empty();
    }
}