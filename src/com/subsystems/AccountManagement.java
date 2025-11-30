package com.subsystems;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.broker.Listener;
import com.broker.Message;
import com.common.dto.LoginRequest;
import com.common.dto.RegistrationRequest;
import com.services.AuthenticationService;
import com.services.SessionManager;
import java.util.HashMap;
import java.util.Map;
import com.entities.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

// Handle USER_REGISTER_REQUESTED, USER_LOGIN_REQUEST and other account events
// !Handle register: 
//Cast payload to a DTO through a registration form. Validate, hash password, insert user via Database.java. On success publish USER_REGISTER_SUCCESS with created User or public USER_REGISTER_FAILED with reason

// !Handle login: 
//Check credentials via AuthenticationService, publish USER_LOGIN_SUCCESS/FAILED and set session info

public class AccountManagement implements Subsystems {
    private AsyncMessageBroker broker;
    private AuthenticationService authService = new AuthenticationService();
    private Listener handleRegister = this::handleRegister;
    private Listener handleLogin = this::handleLogin;

    @Override
    public void init(AsyncMessageBroker broker) {
        this.broker = broker;
        broker.registerListener(EventType.USER_REGISTER_REQUESTED, handleRegister);
        broker.registerListener(EventType.USER_LOGIN_REQUEST, handleLogin);
    }

    @Override
    public void start() {
    }

    @Override
    public void shutdown() {
        broker.unregisterListener(EventType.USER_REGISTER_REQUESTED, handleRegister);
        broker.unregisterListener(EventType.USER_LOGIN_REQUEST, handleLogin);
    }

    private CompletableFuture<Void> handleRegister(Message message) {
        return CompletableFuture.runAsync(() -> {
            if (!(message.getPayload() instanceof RegistrationRequest))
                return;

            RegistrationRequest request = (RegistrationRequest) message.getPayload();

            Optional<String> validationError = authService.validateRegistration(request);

            if (validationError.isPresent()) {
                broker.publish(EventType.USER_REGISTER_FAILED, validationError.get());
                System.out.println("[Account Management] Registration validation failed: " + validationError.get());
                return;
            }

            String hashedPassword = authService.hashPassword(request.getPassword());

            String sql = "INSERT INTO users (username, password, email, role, phone_number, address) VALUES (?, ?, ?, ?, ?, ?)";

            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:shopping_mall.db")) {
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

                statement.setString(1, request.getUsername());
                statement.setString(2, hashedPassword);
                statement.setString(3, request.getEmail());
                statement.setString(4, request.getRole() != null ? request.getRole().name() : null);
                statement.setString(5, request.getPhoneNumber());
                statement.setString(6, request.getAddress());

                int affected = statement.executeUpdate();

                if (affected == 0) {
                    broker.publish(EventType.USER_REGISTER_FAILED, "Insert Command Error");
                    System.out.println("[Account Management] Registration Failed");
                    return;
                }

                int newId = -1;
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next())
                        newId = keys.getInt(1);
                }

                User user = new User(
                        newId,
                        request.getUsername(),
                        request.getEmail(),
                        hashedPassword,
                        request.getRole() != null ? request.getRole() : User.Role.Customer,
                        request.getPhoneNumber(),
                        request.getAddress());

                user.setId(newId);
                // create session and publish safe payload (do not expose password)
                String token = SessionManager.getInstance().createSession(user);
                // clear password before publishing
                user.setPassword(null);

                Map<String, Object> payload = new HashMap<>();
                payload.put("user", user);
                payload.put("token", token);
                broker.publish(EventType.USER_REGISTER_SUCCESS, payload);

                System.out.println("[Account Management] Registration successful: " + request.getUsername());

            } catch (SQLException ex) {
                if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("unique")) {
                    broker.publish(EventType.USER_REGISTER_FAILED, "Unique Error");
                    System.out.println("[Account Management] Username or email already exists");
                    return;
                }
                broker.publish(EventType.USER_REGISTER_FAILED, "Database Error");
                System.out.println("[Account Management] Registration DB error: " + ex.getMessage());
            }
        });
    }

    private CompletableFuture<Void> handleLogin(Message message) {
        return CompletableFuture.runAsync(() -> {
            if (!(message.getPayload() instanceof LoginRequest))
                return;

            LoginRequest request = (LoginRequest) message.getPayload();

            String sql = "SELECT * FROM users WHERE username = ?";

            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:shopping_mall.db")) {
                PreparedStatement statement = connection.prepareStatement(sql);

                statement.setString(1, request.getUsername());

                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        broker.publish(EventType.USER_LOGIN_FAILED, "Invalid Credentials");
                        System.out.println("[Account Management] Login Failed - no such user");
                        return;
                    }

                    String storedHash = result.getString("password");
                    if (!authService.verifyPassword(request.getPassword(), storedHash)) {
                        broker.publish(EventType.USER_LOGIN_FAILED, "Invalid Credentials");
                        System.out.println("[Account Management] Login Failed - Wrong Password");
                        return;
                    }

                    User user = new User(
                            result.getInt("id"),
                            result.getString("username"),
                            result.getString("email"),
                            storedHash,
                            User.Role.valueOf(result.getString("role")),
                            result.getString("phone_number"),
                            result.getString("address"));

                    user.setId(result.getInt("id"));
                    // create session token (managed here) and publish safe payload
                    String token = SessionManager.getInstance().createSession(user);
                    user.setPassword(null);
                    Map<String, Object> loginPayload = new HashMap<>();
                    loginPayload.put("user", user);
                    loginPayload.put("token", token);

                    broker.publish(EventType.USER_LOGIN_SUCCESS, loginPayload);
                    System.out.println("[Account Management] Login Successful - user: " + user.getUserName());
                }
            } catch (SQLException ex) {
                broker.publish(EventType.USER_LOGIN_FAILED, "Database Error");
                System.err.println("[Account Management] Login DB Error " + ex.getMessage());
            }
        });
    }

    public void publishEvent(EventType eventType, Object payload) {
        broker.publish(eventType, payload);
    }
}