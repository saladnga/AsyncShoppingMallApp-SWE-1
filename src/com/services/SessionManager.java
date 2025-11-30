package com.services;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.entities.User;

public class SessionManager {
    private static final SessionManager instance = new SessionManager();

    private final ConcurrentHashMap<String, User> sessions = new ConcurrentHashMap<>();

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return instance;
    };

    public String createSession(User user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, user);
        return token;
    }

    public User getUser(String token) {
        return sessions.get(token);
    };

    public void invalidate(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    };
}
