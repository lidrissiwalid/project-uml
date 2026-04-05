package com.stadiumeats.servlet;

import com.stadiumeats.dao.impl.MenuItemDAOImpl;
import com.stadiumeats.dao.impl.OrderDAOImpl;
import com.stadiumeats.model.Order;
import com.stadiumeats.service.OrderService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OrderServlet — handles /api/orders and /api/orders/{id}/status.
 *
 * POST /api/orders              → CLIENT only — place order
 * GET  /api/orders              → filtered by role
 * PUT  /api/orders/{id}/status  → WORKER or ADMIN
 */
@WebServlet("/api/orders/*")
public class OrderServlet extends BaseApiServlet {

    private final OrderService orderService =
            new OrderService(new OrderDAOImpl(), new MenuItemDAOImpl());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (requireLogin(req, resp)) return;
        if (requireRole(req, resp, "CLIENT")) return;

        Map<?, ?> body = parseBody(req, Map.class);
        if (body == null) {
            badRequest(resp, "Invalid JSON body");
            return;
        }

        Long clientId = getSessionUserId(req);
        try {
            String seatNumber = (String) body.get("seatNumber");
            String paymentMethod = body.get("paymentMethod") != null
                    ? body.get("paymentMethod").toString() : "ONLINE";

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cartItems =
                    (List<Map<String, Object>>) body.get("items");

            Order order = orderService.placeOrder(clientId, seatNumber, cartItems, paymentMethod);
            created(resp, order);
        } catch (IllegalArgumentException e) {
            badRequest(resp, e.getMessage());
        } catch (ClassCastException e) {
            badRequest(resp, "Invalid request format");
        } catch (SQLException e) {
            serverError(resp, "Failed to place order");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (requireLogin(req, resp)) return;

        String role = getSessionRole(req);
        Long userId = getSessionUserId(req);

        try {
            List<Order> orders;
            if ("CLIENT".equals(role)) {
                orders = orderService.getClientOrders(userId);
            } else if ("WORKER".equals(role)) {
                orders = orderService.getActiveOrders();
            } else { // ADMIN
                orders = orderService.getAllOrders();
            }
            ok(resp, orders);
        } catch (SQLException e) {
            serverError(resp, "Failed to retrieve orders");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (requireRole(req, resp, "WORKER", "ADMIN")) return;

        // Path: /{id}/status
        Long orderId = extractOrderId(req);
        if (orderId == null) {
            badRequest(resp, "Invalid order ID in URL");
            return;
        }

        Map<?, ?> body = parseBody(req, Map.class);
        if (body == null) {
            badRequest(resp, "Invalid JSON body");
            return;
        }

        Object statusObj = body.get("status");
        if (statusObj == null) {
            badRequest(resp, "Status is required");
            return;
        }

        try {
            orderService.updateStatus(orderId, statusObj.toString());
            Map<String, String> result = new HashMap<>();
            result.put("message", "Order status updated to " + statusObj);
            ok(resp, result);
        } catch (IllegalArgumentException e) {
            badRequest(resp, e.getMessage());
        } catch (SQLException e) {
            serverError(resp, "Failed to update order status");
        }
    }

    /** Extracts the order ID from paths like "/{id}/status" or "/{id}" */
    private Long extractOrderId(HttpServletRequest req) {
        String pathInfo = req.getPathInfo(); // e.g. "/42/status"
        if (pathInfo == null) return null;
        String[] parts = pathInfo.split("/");
        // parts[0] = "", parts[1] = "42", parts[2] = "status"
        if (parts.length < 2) return null;
        try {
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
