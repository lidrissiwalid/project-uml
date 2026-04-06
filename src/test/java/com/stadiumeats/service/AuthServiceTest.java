package com.stadiumeats.service;

import com.stadiumeats.dao.UserDAO;
import com.stadiumeats.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// BCrypt import for generating hashes inside tests
import org.mindrot.jbcrypt.BCrypt;

/**
 * Unit tests for AuthService — uses Mockito to mock UserDAO.
 * No real database connection required.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserDAO userDAO;

    @InjectMocks
    private AuthService authService;

    // ─── register ────────────────────────────────────────────────

    @Test
    void testRegisterSuccess() throws SQLException {
        // Arrange: no conflicts
        when(userDAO.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userDAO.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(userDAO.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        // Act
        User result = authService.register("newuser", "new@test.com", "password123");

        // Assert
        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("CLIENT", result.getRole());
        assertNull(result.getPasswordHash(), "Password hash must be cleared before returning");
        verify(userDAO).save(any(User.class));
    }

    @Test
    void testRegisterDuplicateUsername() throws SQLException {
        // Arrange: username already taken
        User existing = new User(1L, "existingUser", "exist@test.com", "CLIENT");
        when(userDAO.findByUsername("existingUser")).thenReturn(Optional.of(existing));

        // Act + Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register("existingUser", "other@test.com", "password123"));
        assertEquals("Username already taken", ex.getMessage());
        verify(userDAO, never()).save(any());
    }

    @Test
    void testRegisterDuplicateEmail() throws SQLException {
        // Arrange: email already taken
        when(userDAO.findByUsername("newuser")).thenReturn(Optional.empty());
        User existing = new User(2L, "other", "taken@test.com", "CLIENT");
        when(userDAO.findByEmail("taken@test.com")).thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register("newuser", "taken@test.com", "password123"));
        assertEquals("Email already registered", ex.getMessage());
    }

    @Test
    void testRegisterPasswordTooShort() {
        // No DB call needed — validation fails first
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register("user1", "u1@test.com", "abc"));
        assertTrue(ex.getMessage().contains("6 characters"));
    }

    // ─── login ───────────────────────────────────────────────────

    @Test
    void testLoginSuccess() throws SQLException {
        // Arrange: generate a real BCrypt hash at test time
        String plainPassword = "password123";
        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(4)); // cost=4 for fast tests
        User stored = new User(1L, "client1", "c1@test.com", "CLIENT");
        stored.setPasswordHash(hash);
        when(userDAO.findByUsername("client1")).thenReturn(Optional.of(stored));

        // Act
        User result = authService.login("client1", plainPassword);

        // Assert
        assertNotNull(result);
        assertEquals("client1", result.getUsername());
        assertEquals("CLIENT", result.getRole());
        assertNull(result.getPasswordHash(), "Password hash must be cleared before returning");
    }

    @Test
    void testLoginWrongPassword() throws SQLException {
        String hash = BCrypt.hashpw("password123", BCrypt.gensalt(4));
        User stored = new User(1L, "client1", "c1@test.com", "CLIENT");
        stored.setPasswordHash(hash);
        when(userDAO.findByUsername("client1")).thenReturn(Optional.of(stored));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login("client1", "wrongpassword"));
        assertEquals("Invalid credentials", ex.getMessage());
    }

    @Test
    void testLoginUserNotFound() throws SQLException {
        when(userDAO.findByUsername("ghost")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login("ghost", "password123"));
        assertEquals("Invalid credentials", ex.getMessage());
    }
}
