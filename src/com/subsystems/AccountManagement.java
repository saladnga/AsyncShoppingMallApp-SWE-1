package com.subsystems;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.broker.Listener;
import com.broker.Message;
import com.common.dto.LoginRequest;
import com.common.dto.RegistrationRequest;
import com.services.AuthenticationService;
import com.entities.User;

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
        broker.unregisterListener(EventType.USER_REGISTER_REQUESTED, this::handleRegister);
        broker.unregisterListener(EventType.USER_LOGIN_REQUEST, this::handleLogin);
    }

    private CompletableFuture<Void> handleRegister(Message message) {
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Void> handleLogin(Message message) {
        if (message.getPayload() instanceof LoginRequest) {
            LoginRequest request = (LoginRequest) message.getPayload();

            return authService.loginAsync(request.getUsernameOrEmail(), request.getPassword())
            .thenAccept(user -> {
                if (user != null) {
                    broker.publish(EventType.USER_LOGIN_SUCCESS, user);
                    System.out.println("[Account Management] Login successful - user: " + user.getUserName());
                } else {
                    broker.publish(EventType.USER_LOGIN_FAILED, "Invalid credentials");
                    System.out.println("[Account Management] Login failed");
                }
            });
        }
        return CompletableFuture.completedFuture(null);
    }

    public void publishEvent(EventType eventType, Object payload) {
        broker.publish(eventType, payload);
    }
}