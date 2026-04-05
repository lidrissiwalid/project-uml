package com.stadiumeats.service;

import com.stadiumeats.dao.UserDAO;
import com.stadiumeats.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.Optional;

/**
 * AuthService — Facade over UserDAO.
 * Handles registration and login business logic.
 */
public class AuthService {

    private final UserDAO userDAO;

    // Dependency Injection: service receives DAO via constructor (DIP)
    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Registers a new CLIENT user.
     * @throws IllegalArgumentException on validation failure
     */
    public User register(String username, String email, String plainPassword) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (plainPassword == null || plainPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        if (userDAO.findByUsername(username.trim()).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userDAO.findByEmail(email.trim()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(email.trim());
        user.setPasswordHash(hash);
        user.setRole("CLIENT");

        userDAO.save(user);
        // Clear hash before returning so it is never sent over the wire
        user.setPasswordHash(null);
        return user;
    }

    /**
     * Returns all users (admin only — password hashes stripped in servlet layer).
     */
    public java.util.List<User> listAll() throws SQLException {
        return userDAO.findAll();
    }

    /**
     * Authenticates a user by username + plaintext password.
     * @return authenticated User (without passwordHash)
     * @throws IllegalArgumentException on bad credentials
     */
    public User login(String username, String plainPassword) throws SQLException {
        if (username == null || plainPassword == null) {
            throw new IllegalArgumentException("Username and password are required");
        }
        Optional<User> opt = userDAO.findByUsername(username.trim());
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        User user = opt.get();
        if (!BCrypt.checkpw(plainPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        // Clear hash before returning
        user.setPasswordHash(null);
        return user;
    }
}
