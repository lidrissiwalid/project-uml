package com.stadiumeats.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Generic DAO interface — minimal contract for all DAOs (ISP: only truly generic ops here).
 */
public interface GenericDAO<T> {
    Optional<T> findById(Long id) throws SQLException;
    List<T> findAll() throws SQLException;
    T save(T entity) throws SQLException;
    void update(T entity) throws SQLException;
    void deleteById(Long id) throws SQLException;
}
