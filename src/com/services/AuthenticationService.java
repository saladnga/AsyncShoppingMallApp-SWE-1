package com.services;

import com.entities.User;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AuthenticationService {

    public Optional<User> authenticate(String usernameOrEmail, String password) {
        // Dummy: accept password == "password"
        if ("password".equals(password)) {
            User u = new User(1, usernameOrEmail, usernameOrEmail + "@example.com", null, User.Role.Customer, "", "");
            return Optional.of(u);
        }
        return Optional.empty();
    }

    public CompletableFuture<User> loginAsync(String username, String password) {
        return CompletableFuture
                .supplyAsync(() -> {
                    // This runs in background thread
                    return this.authenticate(username, password).orElse(null);
                })
                .orTimeout(5, java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * Async registration - placeholder for future implementation
     */
    public CompletableFuture<User> registerAsync(String username, String email, String password) {
        return CompletableFuture
                .supplyAsync(() -> {
                    // TODO: Implement registration logic
                    // For now, just create a user
                    if (username != null && email != null && password != null) {
                        return new User(0, username, email, null, User.Role.Customer, "", "");
                    }
                    return null;
                })
                .orTimeout(5, java.util.concurrent.TimeUnit.SECONDS);
    }
}