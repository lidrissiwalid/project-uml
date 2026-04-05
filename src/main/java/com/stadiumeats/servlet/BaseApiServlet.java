package com.stadiumeats.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * BaseApiServlet — Template Method pattern.
 *
 * Skeleton:
 *   readBody() → deserialize via Gson → business logic (abstract) → writeJson()
 *
 * Subclasses override doGet / doPost / doPut / doDelete and call the helper
 * methods provided here to read the request body and write JSON responses.
 */
public abstract class BaseApiServlet extends HttpServlet {

    protected static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .create();

    /* ---- Template helpers ---- */

    /**
     * Reads the raw request body as a String.
     */
    protected String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * Deserializes the request body JSON into the given class.
     * Returns null if the body is empty or malformed.
     */
    protected <T> T parseBody(HttpServletRequest req, Class<T> clazz) throws IOException {
        String body = readBody(req);
        if (body == null || body.trim().isEmpty()) return null;
        try {
            return GSON.fromJson(body, clazz);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    /**
     * Writes a JSON object to the response with the given HTTP status code.
     */
    protected void writeJson(HttpServletResponse resp, int status, Object data) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        try (PrintWriter pw = resp.getWriter()) {
            pw.write(GSON.toJson(data));
        }
    }

    /** Convenience: 200 OK */
    protected void ok(HttpServletResponse resp, Object data) throws IOException {
        writeJson(resp, HttpServletResponse.SC_OK, data);
    }

    /** Convenience: 201 Created */
    protected void created(HttpServletResponse resp, Object data) throws IOException {
        writeJson(resp, HttpServletResponse.SC_CREATED, data);
    }

    /** Error response: {"error": message} */
    protected void error(HttpServletResponse resp, int status, String message) throws IOException {
        Map<String, String> body = new HashMap<>();
        body.put("error", message);
        writeJson(resp, status, body);
    }

    /** 400 Bad Request */
    protected void badRequest(HttpServletResponse resp, String message) throws IOException {
        error(resp, HttpServletResponse.SC_BAD_REQUEST, message);
    }

    /** 401 Unauthorized */
    protected void unauthorized(HttpServletResponse resp, String message) throws IOException {
        error(resp, HttpServletResponse.SC_UNAUTHORIZED, message);
    }

    /** 403 Forbidden */
    protected void forbidden(HttpServletResponse resp, String message) throws IOException {
        error(resp, HttpServletResponse.SC_FORBIDDEN, message);
    }

    /** 404 Not Found */
    protected void notFound(HttpServletResponse resp, String message) throws IOException {
        error(resp, HttpServletResponse.SC_NOT_FOUND, message);
    }

    /** 500 Internal Server Error — never exposes stack trace */
    protected void serverError(HttpServletResponse resp, String message) throws IOException {
        error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
    }

    /**
     * Extract the session attribute "userId" (Long) — returns null if not logged in.
     */
    protected Long getSessionUserId(HttpServletRequest req) {
        Object val = req.getSession(false) != null
                ? req.getSession(false).getAttribute("userId")
                : null;
        if (val instanceof Long) return (Long) val;
        if (val instanceof Number) return ((Number) val).longValue();
        return null;
    }

    /**
     * Extract the session attribute "role" (String) — returns null if not logged in.
     */
    protected String getSessionRole(HttpServletRequest req) {
        if (req.getSession(false) == null) return null;
        Object val = req.getSession(false).getAttribute("role");
        return val != null ? val.toString() : null;
    }

    /**
     * Returns true and calls unauthorized() if no session exists.
     */
    protected boolean requireLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (getSessionUserId(req) == null) {
            unauthorized(resp, "Authentication required");
            return true;
        }
        return false;
    }

    /**
     * Returns true and calls forbidden() if logged-in user does not have one of the required roles.
     */
    protected boolean requireRole(HttpServletRequest req, HttpServletResponse resp,
                                  String... roles) throws IOException {
        String role = getSessionRole(req);
        if (role == null) {
            unauthorized(resp, "Authentication required");
            return true;
        }
        for (String r : roles) {
            if (r.equalsIgnoreCase(role)) return false;
        }
        forbidden(resp, "Insufficient permissions");
        return true;
    }

    /** Handle pre-flight OPTIONS without calling child methods */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws jakarta.servlet.ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        super.service(req, resp);
    }
}
