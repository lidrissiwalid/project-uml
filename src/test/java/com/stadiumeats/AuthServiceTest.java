package com.stadiumeats;

import com.stadiumeats.dao.impl.UserDAOImpl;
import com.stadiumeats.model.User;
import com.stadiumeats.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 * Uses Mockito to mock UserDAO so no real DB is needed.
 */
class AuthServiceTest {

    private UserDAOImpl mockUserDAO;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        mockUserDAO = mock(UserDAOImpl.class);
        authService = new AuthService(mockUserDAO);
    }

    @Test
    void register_shouldThrow_whenUsernameIsBlank() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            authService.register("", "a@b.com", "password123")
        );
        assertEquals("Username is required", ex.getMessage());
    }

    @Test
    void register_shouldThrow_whenPasswordTooShort() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            authService.register("alice", "a@b.com", "12345")
        );
        assertTrue(ex.getMessage().contains("6 characters"));
    }

    @Test
    void login_shouldThrow_whenUserNotFound() throws Exception {
        when(mockUserDAO.findByUsername("ghost")).thenReturn(java.util.Optional.empty());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            authService.login("ghost", "anypass")
        );
        assertEquals("Invalid credentials", ex.getMessage());
    }

    @Test
    void login_shouldThrow_whenPasswordWrong() throws Exception {
        User user = new User();
        user.setUsername("alice");
        // BCrypt hash of "correct_password"
        user.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");

        when(mockUserDAO.findByUsername("alice")).thenReturn(java.util.Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            authService.login("alice", "wrong_password")
        );
        assertEquals("Invalid credentials", ex.getMessage());
    }

    @Test
    void register_shouldThrow_whenUsernameAlreadyTaken() throws Exception {
        User existing = new User();
        existing.setUsername("alice");
        when(mockUserDAO.findByUsername("alice")).thenReturn(java.util.Optional.of(existing));
        when(mockUserDAO.findByEmail("a@b.com")).thenReturn(java.util.Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            authService.register("alice", "a@b.com", "password123")
        );
        assertEquals("Username already taken", ex.getMessage());
    }
}
