package com.stadiumeats.servlet;

import com.stadiumeats.dao.impl.UserDAOImpl;
import com.stadiumeats.model.User;
import com.stadiumeats.service.AuthService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * AuthServlet — handles /api/auth/login and /api/auth/register.
 * Uses BaseApiServlet's Template Method helpers.
 */
@WebServlet("/api/auth/*")
public class AuthServlet extends BaseApiServlet {

    private final AuthService authService = new AuthService(new UserDAOImpl());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if ("/users".equals(path)) {
            if (requireRole(req, resp, "ADMIN")) return;
            try {
                java.util.List<com.stadiumeats.model.User> users = authService.listAll();
                // Strip password hashes
                users.forEach(u -> u.setPasswordHash(null));
                ok(resp, users);
            } catch (java.sql.SQLException e) {
                serverError(resp, "Database error");
            }
        } else {
            notFound(resp, "Unknown endpoint");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo(); // "/login" or "/register"

        if ("/login".equals(path)) {
            handleLogin(req, resp);
        } else if ("/register".equals(path)) {
            handleRegister(req, resp);
        } else if ("/logout".equals(path)) {
            handleLogout(req, resp);
        } else {
            notFound(resp, "Unknown auth endpoint");
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, String> body = parseBody(req, Map.class);
        if (body == null) {
            badRequest(resp, "Invalid JSON body");
            return;
        }
        String username = body.get("username");
        String password = body.get("password");

        try {
            User user = authService.login(username, password);
            // Create session and store user info
            HttpSession session = req.getSession(true);
            session.setAttribute("userId",   user.getId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("role",     user.getRole());
            session.setMaxInactiveInterval(3600); // 1 hour

            Map<String, Object> response = buildUserMap(user);
            ok(resp, response);
        } catch (IllegalArgumentException e) {
            unauthorized(resp, e.getMessage());
        } catch (SQLException e) {
            serverError(resp, "Database error during login");
        }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, String> body = parseBody(req, Map.class);
        if (body == null) {
            badRequest(resp, "Invalid JSON body");
            return;
        }
        String username = body.get("username");
        String email    = body.get("email");
        String password = body.get("password");

        try {
            User user = authService.register(username, email, password);
            // Auto-login after registration
            HttpSession session = req.getSession(true);
            session.setAttribute("userId",   user.getId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("role",     user.getRole());
            session.setMaxInactiveInterval(3600);

            created(resp, buildUserMap(user));
        } catch (IllegalArgumentException e) {
            badRequest(resp, e.getMessage());
        } catch (SQLException e) {
            serverError(resp, "Database error during registration");
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        Map<String, String> result = new HashMap<>();
        result.put("message", "Logged out successfully");
        ok(resp, result);
    }

    private Map<String, Object> buildUserMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id",       user.getId());
        map.put("username", user.getUsername());
        map.put("email",    user.getEmail());
        map.put("role",     user.getRole());
        return map;
    }
}
