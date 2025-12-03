package com.subsystems;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.broker.Message;
import com.common.dto.auth.LoginRequest;
import com.common.dto.auth.RegistrationRequest;
import com.common.dto.account.AccountViewRequest;
import com.services.SessionManager;
import java.util.HashMap;
import java.util.Map;
import com.entities.User;

import java.util.concurrent.CompletableFuture;
import com.managers.account.RegisterManager;
import com.managers.account.LoginManager;
import com.managers.account.ViewAccountManager;
import com.managers.account.EditAccountManager;
import com.common.dto.account.AccountEditRequest;

// Handle USER_REGISTER_REQUESTED, USER_LOGIN_REQUEST and other account events
// !Handle register: 
//Cast payload to a DTO through a registration form. Validate, hash password, insert user via Database.java. On success publish USER_REGISTER_SUCCESS with created User or public USER_REGISTER_FAILED with reason

// !Handle login: 
//Check credentials via AuthenticationService, publish USER_LOGIN_SUCCESS/FAILED and set session info

public class AccountManagement implements Subsystems {
    private final RegisterManager registerManager;
    private final LoginManager loginManager;
    private final ViewAccountManager viewAccountManager;
    private final EditAccountManager editAccountManager;
    private AsyncMessageBroker broker;

    public AccountManagement(RegisterManager rm, LoginManager lm, ViewAccountManager vam, EditAccountManager eam) {
        this.registerManager = rm;
        this.loginManager = lm;
        this.viewAccountManager = vam;
        this.editAccountManager = eam;
    }

    public void publishEvent(EventType eventType, Object payload) {
        broker.publish(eventType, payload);
    }

    @Override
    public void init(AsyncMessageBroker broker) {
        this.broker = broker;
        broker.registerListener(EventType.USER_REGISTER_REQUESTED, this::handleRegister);
        broker.registerListener(EventType.USER_LOGIN_REQUEST, this::handleLogin);
        broker.registerListener(EventType.ACCOUNT_VIEW_REQUESTED, this::handleAccountView);
        broker.registerListener(EventType.ACCOUNT_EDIT_REQUESTED, this::handleAccountEdit);
    }

    @Override
    public void start() {
    }

    @Override
    public void shutdown() {
        broker.unregisterListener(EventType.USER_REGISTER_REQUESTED, this::handleRegister);
        broker.unregisterListener(EventType.USER_LOGIN_REQUEST, this::handleLogin);
        broker.unregisterListener(EventType.ACCOUNT_VIEW_REQUESTED, this::handleAccountView);
        broker.unregisterListener(EventType.ACCOUNT_EDIT_REQUESTED, this::handleAccountEdit);
    }

    private CompletableFuture<Void> handleRegister(Message message) {
        return CompletableFuture.runAsync(() -> {
            // Null handling - check for null payload
            if (message == null || message.getPayload() == null) {
                broker.publish(EventType.USER_REGISTER_FAILED, "Internal Error: Invalid registration request");
                System.out.println("[AccountManagement] Registration failed: null request");
                return;
            }

            if (!(message.getPayload() instanceof RegistrationRequest request)) {
                broker.publish(EventType.USER_REGISTER_FAILED, "Internal Error: Invalid registration request");
                return;
            }

            // Null handling - validate request fields
            if (request.getUsername() == null || request.getEmail() == null ||
                    request.getPassword() == null || request.getRole() == null) {
                broker.publish(EventType.USER_REGISTER_FAILED, "Internal Error: Missing required fields");
                System.out.println("[AccountManagement] Registration failed: null fields");
                return;
            }

            try {
                User newUser = registerManager.register(request);
                String token = SessionManager.getInstance().createSession(newUser);
                newUser.setPassword(null);

                Map<String, Object> payload = new HashMap<>();
                payload.put("user", newUser);
                payload.put("token", token);
                broker.publish(EventType.USER_REGISTER_SUCCESS, payload);
                System.out.println("[AccountManagement] Register Success");

            } catch (IllegalArgumentException ex) {
                // Handle IllegalArgumentException for null/invalid inputs
                broker.publish(EventType.USER_REGISTER_FAILED, "Internal Error: " + ex.getMessage());
                System.out.println("[AccountManagement] Registration validation error: " + ex.getMessage());
            } catch (NullPointerException ex) {
                // Handle NullPointerException
                broker.publish(EventType.USER_REGISTER_FAILED, "Internal Error: Null pointer exception");
                System.out.println("[AccountManagement] Registration null pointer error: " + ex.getMessage());
            } catch (Exception ex) {
                // Duplicate email - pass through the actual error message
                String errorMsg = ex.getMessage();
                if (errorMsg != null && errorMsg.contains("already")) {
                    broker.publish(EventType.USER_REGISTER_FAILED, errorMsg);
                } else {
                    broker.publish(EventType.USER_REGISTER_FAILED, "Database Error: " + errorMsg);
                }
                System.out.println("[AccountManagement] Registration error: " + errorMsg);
            }
        });
    }

    private CompletableFuture<Void> handleLogin(Message message) {
        return CompletableFuture.runAsync(() -> {
            if (message == null || message.getPayload() == null) {
                broker.publish(EventType.USER_LOGIN_FAILED, "Invalid login request");
                return;
            }

            if (!(message.getPayload() instanceof LoginRequest request)) {
                broker.publish(EventType.USER_LOGIN_FAILED, "Invalid login request");
                return;
            }

            try {
                User user = loginManager.login(request.getUsername(), request.getPassword());

                String token = SessionManager.getInstance().createSession(user);
                user.setPassword(null);

                Map<String, Object> payload = new HashMap<>();
                payload.put("user", user);
                payload.put("token", token);
                broker.publish(EventType.USER_LOGIN_SUCCESS, payload);
                System.out.println("[AccountManagement] Login Success");
            } catch (Exception ex) {
                // Login Failure - pass through actual error message
                String errorMsg = ex.getMessage();
                if (errorMsg != null && (errorMsg.contains("not found") || errorMsg.contains("Incorrect"))) {
                    broker.publish(EventType.USER_LOGIN_FAILED, "Invalid credentials");
                } else {
                    broker.publish(EventType.USER_LOGIN_FAILED, "Database Error: " + errorMsg);
                }
                System.out.println("[AccountManagement] Login error: " + errorMsg);
            }
        });
    }

    private CompletableFuture<Void> handleAccountView(Message message) {
        return CompletableFuture.runAsync(() -> {
            if (!(message.getPayload() instanceof AccountViewRequest request))
                return;

            try {
                User user = viewAccountManager.viewAccount(request.getUserId());
                if (user != null) {
                    user.setPassword(null); // Don't send password
                    broker.publish(EventType.ACCOUNT_VIEW_RETURNED, user);
                    System.out.println("[AccountManagement] Account view returned for user: " + user.getUsername());
                } else {
                    broker.publish(EventType.ACCOUNT_VIEW_RETURNED, null);
                    System.out.println("[AccountManagement] User not found for account view");
                }
            } catch (Exception ex) {
                broker.publish(EventType.ACCOUNT_VIEW_RETURNED, null);
                System.out.println("[AccountManagement] Account view error: " + ex.getMessage());
            }
        });
    }

    private CompletableFuture<Void> handleAccountEdit(Message message) {
        return CompletableFuture.runAsync(() -> {
            if (message == null || message.getPayload() == null) {
                broker.publish(EventType.ACCOUNT_UPDATE_FAILED, "Invalid edit request");
                return;
            }

            if (!(message.getPayload() instanceof AccountEditRequest request)) {
                broker.publish(EventType.ACCOUNT_UPDATE_FAILED, "Invalid edit request payload");
                return;
            }

            // Validate that at least one field is being updated
            if (request.getNewUsername() == null && request.getNewEmail() == null && 
                request.getNewPassword() == null && request.getNewPhone() == null && 
                request.getNewAddress() == null) {
                broker.publish(EventType.ACCOUNT_UPDATE_FAILED, "No fields to update");
                return;
            }

            try {
                // Get the user who is making the edit (from session or request)
                // For now, we'll get the user from the repository
                User editor = viewAccountManager.viewAccount(request.getUserId());
                if (editor == null) {
                    broker.publish(EventType.ACCOUNT_UPDATE_FAILED, "User not found");
                    return;
                }

                User updatedUser = editAccountManager.editUser(
                        editor,
                        request.getUserId(),
                        request.getNewUsername(),
                        request.getNewEmail(),
                        request.getNewPassword(),
                        request.getNewPhone(),
                        request.getNewAddress());

                if (updatedUser != null) {
                    updatedUser.setPassword(null); // Don't send password
                    broker.publish(EventType.ACCOUNT_UPDATE_SUCCESS, updatedUser);
                    System.out.println(
                            "[AccountManagement] Account updated successfully for user: " + updatedUser.getUsername());
                } else {
                    broker.publish(EventType.ACCOUNT_UPDATE_FAILED, "Failed to update account");
                    System.out.println("[AccountManagement] Account update failed: user not found");
                }
            } catch (Exception ex) {
                broker.publish(EventType.ACCOUNT_UPDATE_FAILED, "Error: " + ex.getMessage());
                System.out.println("[AccountManagement] Account update error: " + ex.getMessage());
            }
        });
    }
}