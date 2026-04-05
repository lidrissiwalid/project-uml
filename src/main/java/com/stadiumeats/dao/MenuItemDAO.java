package com.stadiumeats.dao;

import com.stadiumeats.model.MenuItem;
import java.sql.SQLException;
import java.util.List;

public interface MenuItemDAO extends GenericDAO<MenuItem> {
    List<MenuItem> findAllAvailable() throws SQLException;
    List<MenuItem> findByCategory(String category) throws SQLException;
}
