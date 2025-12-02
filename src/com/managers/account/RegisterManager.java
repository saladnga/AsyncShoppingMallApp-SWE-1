package com.managers.account;

import java.util.Optional;

import com.common.dto.auth.RegistrationRequest;
import com.entities.User;
import com.repository.UserRepository;
import com.services.AuthenticationService;

/**
 * Handles business logic for user registration.
 */
public class RegisterManager {

    private final UserRepository userRepo;
    private final AuthenticationService auth;

    public RegisterManager(UserRepository userRepo, AuthenticationService auth) {
        this.userRepo = userRepo;
        this.auth = auth;
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
        
        // TC08: Password Regex Boundaries - specific validation messages
        // Note: The regex checks above already cover all cases, but we can make messages more specific

        if (userRepo.existsUsername(username) || userRepo.existsEmail(email)) {
            return Optional.of("Username or email already in use");
        }

        return Optional.empty();
    }

    public User register(RegistrationRequest request) throws Exception {

        Optional<String> error = validateRegistration(request);
        if (error.isPresent()) {
            throw new Exception(error.get());
        }

        String hashed = auth.hashPassword(request.getPassword());

        // Build user using Builder pattern
        User newUser = new User.Builder()
                .setUsername(request.getUsername())
                .setEmail(request.getEmail())
                .setHashedPassword(hashed)
                .setPhoneNumber(request.getPhoneNumber())
                .setAddress(request.getAddress())
                .setRole(request.getRole())
                .build();

        // Insert into DB
        int id = userRepo.insert(newUser);
        newUser.setId(id);

        return newUser;
    }
}