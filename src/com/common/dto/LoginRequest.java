package com.common.dto;

public class LoginRequest {
    private final String usernameOrEmail;
    private final String password;

    public LoginRequest(String usernameOrEmail, String password) {
        this.usernameOrEmail = usernameOrEmail;
        this.password = password;
    }

    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }

    public String getPassword() {
        return password;
    }

    public String toString() {
        return "LoginRequest[user=" + usernameOrEmail + "]";
    }
}