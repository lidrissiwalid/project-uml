package com.stadiumeats.dao;

import com.stadiumeats.model.User;
import java.sql.SQLException;
import java.util.Optional;

public interface UserDAO extends GenericDAO<User> {
    Optional<User> findByUsername(String username) throws SQLException;
    Optional<User> findByEmail(String email) throws SQLException;
}
