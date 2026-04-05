package com.stadiumeats.dao.impl;

import com.stadiumeats.dao.MenuItemDAO;
import com.stadiumeats.model.MenuItem;
import com.stadiumeats.util.ConnectionFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MenuItemDAOImpl implements MenuItemDAO {

    private MenuItem mapRow(ResultSet rs) throws SQLException {
        MenuItem m = new MenuItem();
        m.setId(rs.getLong("id"));
        m.setName(rs.getString("name"));
        m.setDescription(rs.getString("description"));
        m.setPrice(rs.getBigDecimal("price"));
        m.setCategory(rs.getString("category"));
        m.setImageUrl(rs.getString("image_url"));
        m.setAvailable(rs.getBoolean("available"));
        return m;
    }

    @Override
    public Optional<MenuItem> findById(Long id) throws SQLException {
        String sql = "SELECT id, name, description, price, category, image_url, available FROM menu_items WHERE id = ?";
        try (Connection c = ConnectionFactory.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<MenuItem> findAll() throws SQLException {
        String sql = "SELECT id, name, description, price, category, image_url, available FROM menu_items ORDER BY category, name";
        List<MenuItem> list = new ArrayList<>();
        try (Connection c = ConnectionFactory.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public List<MenuItem> findAllAvailable() throws SQLException {
        String sql = "SELECT id, name, description, price, category, image_url, available FROM menu_items WHERE available = TRUE ORDER BY category, name";
        List<MenuItem> list = new ArrayList<>();
        try (Connection c = ConnectionFactory.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public List<MenuItem> findByCategory(String category) throws SQLException {
        String sql = "SELECT id, name, description, price, category, image_url, available FROM menu_items WHERE category = ? ORDER BY name";
        List<MenuItem> list = new ArrayList<>();
        try (Connection c = ConnectionFactory.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public MenuItem save(MenuItem item) throws SQLException {
        String sql = "INSERT INTO menu_items (name, description, price, category, image_url, available) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = ConnectionFactory.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setBigDecimal(3, item.getPrice());
            ps.setString(4, item.getCategory());
            ps.setString(5, item.getImageUrl());
            ps.setBoolean(6, item.isAvailable());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) item.setId(keys.getLong(1));
            }
        }
        return item;
    }

    @Override
    public void update(MenuItem item) throws SQLException {
        String sql = "UPDATE menu_items SET name = ?, description = ?, price = ?, category = ?, image_url = ?, available = ? WHERE id = ?";
        try (Connection c = ConnectionFactory.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setBigDecimal(3, item.getPrice());
            ps.setString(4, item.getCategory());
            ps.setString(5, item.getImageUrl());
            ps.setBoolean(6, item.isAvailable());
            ps.setLong(7, item.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteById(Long id) throws SQLException {
        String sql = "DELETE FROM menu_items WHERE id = ?";
        try (Connection c = ConnectionFactory.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}
