package com.stadiumeats.dao.impl;

import com.stadiumeats.dao.OrderDAO;
import com.stadiumeats.model.Order;
import com.stadiumeats.model.OrderItem;
import com.stadiumeats.util.ConnectionFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrderDAOImpl implements OrderDAO {

    private Order mapRow(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getLong("id"));
        o.setClientId(rs.getLong("client_id"));
        o.setClientUsername(rs.getString("client_username"));
        o.setSeatNumber(rs.getString("seat_number"));
        o.setStatus(rs.getString("status"));
        o.setTotalPrice(rs.getBigDecimal("total_price"));
        o.setPaymentMethod(rs.getString("payment_method"));
        o.setCreatedAt(rs.getString("created_at"));
        return o;
    }

    private List<OrderItem> loadItems(Long orderId) throws SQLException {
        String sql = "SELECT oi.id, oi.order_id, oi.menu_item_id, mi.name AS menu_item_name, " +
                     "oi.quantity, oi.unit_price " +
                     "FROM order_items oi " +
                     "JOIN menu_items mi ON oi.menu_item_id = mi.id " +
                     "WHERE oi.order_id = ?";
        List<OrderItem> items = new ArrayList<>();
        try (Connection c = ConnectionFactory.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItem oi = new OrderItem();
                    oi.setId(rs.getLong("id"));
                    oi.setOrderId(rs.getLong("order_id"));
                    oi.setMenuItemId(rs.getLong("menu_item_id"));
                    oi.setMenuItemName(rs.getString("menu_item_name"));
                    oi.setQuantity(rs.getInt("quantity"));
                    oi.setUnitPrice(rs.getBigDecimal("unit_price"));
                    items.add(oi);
                }
            }
        }
        return items;
    }

    private List<Order> queryOrders(String sql, Object... params) throws SQLException {
        List<Order> list = new ArrayList<>();
        try (Connection c = ConnectionFactory.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order o = mapRow(rs);
                    o.setItems(loadItems(o.getId()));
                    list.add(o);
                }
            }
        }
        return list;
    }

    private static final String BASE_SELECT =
        "SELECT o.id, o.client_id, u.username AS client_username, o.seat_number, " +
        "o.status, o.total_price, o.payment_method, " +
        "DATE_FORMAT(o.created_at, '%Y-%m-%d %H:%i:%s') AS created_at " +
        "FROM orders o JOIN users u ON o.client_id = u.id ";

    @Override
    public Optional<Order> findById(Long id) throws SQLException {
        List<Order> result = queryOrders(BASE_SELECT + "WHERE o.id = ?", id);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<Order> findAll() throws SQLException {
        return queryOrders(BASE_SELECT + "ORDER BY o.created_at DESC");
    }

    @Override
    public List<Order> findByClientId(Long clientId) throws SQLException {
        return queryOrders(BASE_SELECT + "WHERE o.client_id = ? ORDER BY o.created_at DESC", clientId);
    }

    @Override
    public List<Order> findByStatus(String status) throws SQLException {
        return queryOrders(BASE_SELECT + "WHERE o.status = ? ORDER BY o.created_at ASC", status);
    }

    @Override
    public void updateStatus(Long orderId, String status) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection c = ConnectionFactory.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, orderId);
            ps.executeUpdate();
        }
    }

    @Override
    public Order save(Order order) throws SQLException {
        String sql = "INSERT INTO orders (client_id, seat_number, status, total_price, payment_method) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = ConnectionFactory.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, order.getClientId());
            ps.setString(2, order.getSeatNumber());
            ps.setString(3, order.getStatus() != null ? order.getStatus() : "PENDING");
            ps.setBigDecimal(4, order.getTotalPrice());
            ps.setString(5, order.getPaymentMethod());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) order.setId(keys.getLong(1));
            }
        }
        return order;
    }

    @Override
    public Order saveWithItems(Order order, List<OrderItem> items) throws SQLException {
        String orderSql = "INSERT INTO orders (client_id, seat_number, status, total_price, payment_method) " +
                          "VALUES (?, ?, 'PENDING', ?, ?)";
        String itemSql  = "INSERT INTO order_items (order_id, menu_item_id, quantity, unit_price) VALUES (?, ?, ?, ?)";

        try (Connection c = ConnectionFactory.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                long orderId;
                try (PreparedStatement ps = c.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, order.getClientId());
                    ps.setString(2, order.getSeatNumber());
                    ps.setBigDecimal(3, order.getTotalPrice());
                    ps.setString(4, order.getPaymentMethod());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        orderId = keys.getLong(1);
                    }
                }
                order.setId(orderId);
                order.setStatus("PENDING");

                try (PreparedStatement ps = c.prepareStatement(itemSql)) {
                    for (OrderItem oi : items) {
                        ps.setLong(1, orderId);
                        ps.setLong(2, oi.getMenuItemId());
                        ps.setInt(3, oi.getQuantity());
                        ps.setBigDecimal(4, oi.getUnitPrice());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
        return order;
    }

    @Override
    public void update(Order order) throws SQLException {
        updateStatus(order.getId(), order.getStatus());
    }

    @Override
    public void deleteById(Long id) throws SQLException {
        String sql = "DELETE FROM orders WHERE id = ?";
        try (Connection c = ConnectionFactory.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
