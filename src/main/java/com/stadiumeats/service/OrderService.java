package com.stadiumeats.service;

import com.stadiumeats.dao.MenuItemDAO;
import com.stadiumeats.dao.OrderDAO;
import com.stadiumeats.model.MenuItem;
import com.stadiumeats.model.Order;
import com.stadiumeats.model.OrderItem;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OrderService — Facade over OrderDAO and MenuItemDAO.
 * Receives DAOs via constructor injection (DIP).
 * Uses PaymentStrategy for payment processing (OCP — new payment = new class).
 */
public class OrderService {

    private final OrderDAO orderDAO;
    private final MenuItemDAO menuItemDAO;

    // Dependency Injection
    public OrderService(OrderDAO orderDAO, MenuItemDAO menuItemDAO) {
        this.orderDAO = orderDAO;
        this.menuItemDAO = menuItemDAO;
    }

    /**
     * Place a new order.
     * @param clientId     ID of the authenticated client
     * @param seatNumber   seat number from the request body
     * @param cartItems    list of {menuItemId, quantity} maps
     * @param paymentMethodLabel "ONLINE" or "CASH"
     * @return the persisted Order with its items
     */
    public Order placeOrder(Long clientId,
                            String seatNumber,
                            List<Map<String, Object>> cartItems,
                            String paymentMethodLabel) throws SQLException {

        if (seatNumber == null || seatNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Seat number is required");
        }
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        // Select payment strategy (OCP: new payment = new class)
        PaymentStrategy strategy = resolveStrategy(paymentMethodLabel);

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map<String, Object> entry : cartItems) {
            long menuItemId;
            int quantity;
            try {
                menuItemId = ((Number) entry.get("menuItemId")).longValue();
                quantity   = ((Number) entry.get("quantity")).intValue();
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid cart item format");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }

            Optional<MenuItem> optItem = menuItemDAO.findById(menuItemId);
            if (optItem.isEmpty()) {
                throw new IllegalArgumentException("Menu item not found: " + menuItemId);
            }
            MenuItem item = optItem.get();
            if (!item.isAvailable()) {
                throw new IllegalArgumentException("Item not available: " + item.getName());
            }

            OrderItem oi = new OrderItem();
            oi.setMenuItemId(item.getId());
            oi.setMenuItemName(item.getName());
            oi.setQuantity(quantity);
            oi.setUnitPrice(item.getPrice());
            orderItems.add(oi);

            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }

        // Simulate payment processing
        strategy.processPayment(total);

        Order order = new Order();
        order.setClientId(clientId);
        order.setSeatNumber(seatNumber.trim());
        order.setTotalPrice(total);
        order.setPaymentMethod(strategy.getMethodName());
        order.setStatus("PENDING");

        Order saved = orderDAO.saveWithItems(order, orderItems);
        saved.setItems(orderItems);
        return saved;
    }

    /** Returns all orders for a given client. */
    public List<Order> getClientOrders(Long clientId) throws SQLException {
        return orderDAO.findByClientId(clientId);
    }

    /** Returns all orders (admin view). */
    public List<Order> getAllOrders() throws SQLException {
        return orderDAO.findAll();
    }

    /** Returns pending + in-delivery orders (worker view). */
    public List<Order> getActiveOrders() throws SQLException {
        List<Order> result = new ArrayList<>();
        result.addAll(orderDAO.findByStatus("PENDING"));
        result.addAll(orderDAO.findByStatus("IN_DELIVERY"));
        return result;
    }

    /**
     * Update order status. Role validation is enforced in the servlet layer.
     */
    public void updateStatus(Long orderId, String newStatus) throws SQLException {
        List<String> valid = List.of("PENDING", "CONFIRMED", "IN_DELIVERY", "DELIVERED", "CANCELLED");
        if (!valid.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid status: " + newStatus);
        }
        // Verify order exists
        Optional<Order> opt = orderDAO.findById(orderId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        orderDAO.updateStatus(orderId, newStatus);
    }

    /* ---- private helpers ---- */

    private PaymentStrategy resolveStrategy(String label) {
        if ("CASH".equalsIgnoreCase(label)) {
            return new CashPaymentStrategy();
        }
        // Default to online
        return new OnlinePaymentStrategy();
    }
}
