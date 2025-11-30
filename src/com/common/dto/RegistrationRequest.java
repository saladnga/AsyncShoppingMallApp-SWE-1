package com.common.dto;

import com.entities.User.Role;

public class RegistrationRequest {
    public String username;
    public String email;
    public String password;
    public String phoneNumber;
    public String address;
    public Role role;

    public RegistrationRequest(String username, String email, String password, Role role, String phoneNumber, String address) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.phoneNumber = phoneNumber;
        this.address = address;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public Role getRole() {
        return role;
    }

    public String toString() {
        return "RegistrationRequest[user=" + username + ", email=" + email + "]";
    }
}
