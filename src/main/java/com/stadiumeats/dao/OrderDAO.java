package com.stadiumeats.dao;

import com.stadiumeats.model.Order;
import com.stadiumeats.model.OrderItem;
import java.sql.SQLException;
import java.util.List;

public interface OrderDAO extends GenericDAO<Order> {
    List<Order> findByClientId(Long clientId) throws SQLException;
    List<Order> findByStatus(String status) throws SQLException;
    void updateStatus(Long orderId, String status) throws SQLException;
    Order saveWithItems(Order order, List<OrderItem> items) throws SQLException;
}
