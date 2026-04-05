package com.stadiumeats.servlet;

import com.stadiumeats.dao.MenuItemDAO;
import com.stadiumeats.dao.impl.MenuItemDAOImpl;
import com.stadiumeats.model.MenuItem;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MenuServlet — handles /api/menu and /api/menu/{id}.
 *
 * GET  /api/menu       → public: return available items (admin sees all)
 * POST /api/menu       → ADMIN only
 * PUT  /api/menu/{id}  → ADMIN only
 * DELETE /api/menu/{id}→ ADMIN only
 */
@WebServlet("/api/menu/*")
public class MenuServlet extends BaseApiServlet {

    private final MenuItemDAO menuItemDAO = new MenuItemDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String role = getSessionRole(req);
            List<MenuItem> items = "ADMIN".equals(role)
                    ? menuItemDAO.findAll()
                    : menuItemDAO.findAllAvailable();
            ok(resp, items);
        } catch (SQLException e) {
            serverError(resp, "Failed to load menu");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (requireRole(req, resp, "ADMIN")) return;

        Map<?, ?> body = parseBody(req, Map.class);
        if (body == null) {
            badRequest(resp, "Invalid JSON body");
            return;
        }

        try {
            MenuItem item = mapToItem(body, null);
            menuItemDAO.save(item);
            created(resp, item);
        } catch (IllegalArgumentException e) {
            badRequest(resp, e.getMessage());
        } catch (SQLException e) {
            serverError(resp, "Failed to create menu item");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (requireRole(req, resp, "ADMIN")) return;

        Long id = extractId(req);
        if (id == null) {
            badRequest(resp, "Missing menu item ID in URL");
            return;
        }

        Map<?, ?> body = parseBody(req, Map.class);
        if (body == null) {
            badRequest(resp, "Invalid JSON body");
            return;
        }

        try {
            Optional<MenuItem> existing = menuItemDAO.findById(id);
            if (existing.isEmpty()) {
                notFound(resp, "Menu item not found");
                return;
            }
            MenuItem item = mapToItem(body, existing.get());
            item.setId(id);
            menuItemDAO.update(item);
            ok(resp, item);
        } catch (IllegalArgumentException e) {
            badRequest(resp, e.getMessage());
        } catch (SQLException e) {
            serverError(resp, "Failed to update menu item");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (requireRole(req, resp, "ADMIN")) return;

        Long id = extractId(req);
        if (id == null) {
            badRequest(resp, "Missing menu item ID in URL");
            return;
        }

        try {
            Optional<MenuItem> existing = menuItemDAO.findById(id);
            if (existing.isEmpty()) {
                notFound(resp, "Menu item not found");
                return;
            }
            menuItemDAO.deleteById(id);
            Map<String, String> result = new HashMap<>();
            result.put("message", "Menu item deleted");
            ok(resp, result);
        } catch (SQLException e) {
            serverError(resp, "Failed to delete menu item");
        }
    }

    /* ---- helpers ---- */

    /** Extracts /{id} from path info like "/42" */
    private Long extractId(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) return null;
        try {
            return Long.parseLong(pathInfo.substring(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Maps a JSON body map to a MenuItem, merging with existing if updating. */
    private MenuItem mapToItem(Map<?, ?> body, MenuItem existing) {
        MenuItem item = existing != null ? existing : new MenuItem();

        if (body.containsKey("name")) {
            String name = (String) body.get("name");
            if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Name is required");
            item.setName(name.trim());
        } else if (existing == null) {
            throw new IllegalArgumentException("Name is required");
        }

        if (body.containsKey("description")) {
            item.setDescription((String) body.get("description"));
        }

        if (body.containsKey("price")) {
            Object priceObj = body.get("price");
            if (priceObj == null) throw new IllegalArgumentException("Price is required");
            try {
                item.setPrice(new BigDecimal(priceObj.toString()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid price format");
            }
        } else if (existing == null) {
            throw new IllegalArgumentException("Price is required");
        }

        if (body.containsKey("category")) {
            String cat = (String) body.get("category");
            if (cat == null || cat.trim().isEmpty()) throw new IllegalArgumentException("Category is required");
            item.setCategory(cat.trim());
        } else if (existing == null) {
            throw new IllegalArgumentException("Category is required");
        }

        if (body.containsKey("imageUrl")) {
            item.setImageUrl((String) body.get("imageUrl"));
        }

        if (body.containsKey("available")) {
            Object av = body.get("available");
            item.setAvailable(Boolean.parseBoolean(av != null ? av.toString() : "true"));
        } else if (existing == null) {
            item.setAvailable(true);
        }

        return item;
    }
}
