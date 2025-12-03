package com.managers.account;

import com.entities.User;
import com.repository.UserRepository;

/**
 * Loads account info and handles permission rules.
 */

public class ViewAccountManager {

    private final UserRepository repo;

    public ViewAccountManager(UserRepository repo) {
        this.repo = repo;
    }

    public User viewAccount(int userId, User.Role requesterRole) throws Exception {

        User user = repo.findById(userId);

        if (user == null)
            throw new Exception("User not found");

        // CEO can view everything
        if (requesterRole == User.Role.CEO)
            return user;

        // STAFF cannot view CEO data
        if (requesterRole == User.Role.STAFF && user.getRole() == User.Role.CEO)
            throw new Exception("Access denied");

        // CUSTOMER can only view own account
        if (requesterRole == User.Role.CUSTOMER && user.getId() != userId)
            throw new Exception("Permission denied");

        return user;
    }

    // Convenience method for users viewing their own account
    public User viewAccount(int userId) throws Exception {
        User user = repo.findById(userId);
        if (user == null)
            throw new Exception("User not found");
        return user;
    }
}