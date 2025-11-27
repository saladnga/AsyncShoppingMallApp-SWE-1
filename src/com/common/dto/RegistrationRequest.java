package com.common.dto;

public class RegistrationRequest {
    public String username;
    public String email;
    public String password;

    public RegistrationRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String toString() {
        return "RegistrationRequest[user=" + username + ", email=" + email + "]";
    }
}
