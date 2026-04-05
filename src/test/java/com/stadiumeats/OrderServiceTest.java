package com.stadiumeats;

import com.stadiumeats.dao.impl.MenuItemDAOImpl;
import com.stadiumeats.dao.impl.OrderDAOImpl;
import com.stadiumeats.model.MenuItem;
import com.stadiumeats.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderService.
 * Uses Mockito to mock DAO dependencies.
 */
class OrderServiceTest {

    private OrderDAOImpl mockOrderDAO;
    private MenuItemDAOImpl mockMenuItemDAO;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        mockOrderDAO    = mock(OrderDAOImpl.class);
        mockMenuItemDAO = mock(MenuItemDAOImpl.class);
        orderService    = new OrderService(mockOrderDAO, mockMenuItemDAO);
    }

    @Test
    void placeOrder_shouldThrow_whenSeatIsBlank() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            orderService.placeOrder(1L, "", List.of(), "CASH")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("seat"));
    }

    @Test
    void placeOrder_shouldThrow_whenCartIsEmpty() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            orderService.placeOrder(1L, "A-12", List.of(), "CASH")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("empty") ||
                   ex.getMessage().toLowerCase().contains("item"));
    }

    @Test
    void placeOrder_shouldThrow_whenMenuItemNotFound() throws Exception {
        when(mockMenuItemDAO.findById(99L)).thenReturn(Optional.empty());

        List<Map<String, Object>> items = List.of(Map.of("menuItemId", 99, "quantity", 1));

        assertThrows(Exception.class, () ->
            orderService.placeOrder(1L, "B-05", items, "ONLINE")
        );
    }

    @Test
    void placeOrder_shouldThrow_whenMenuItemUnavailable() throws Exception {
        MenuItem item = new MenuItem();
        item.setId(1L);
        item.setName("Burger");
        item.setPrice(9.99);
        item.setAvailable(false); // not available

        when(mockMenuItemDAO.findById(1L)).thenReturn(Optional.of(item));

        List<Map<String, Object>> items = List.of(Map.of("menuItemId", 1, "quantity", 2));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            orderService.placeOrder(1L, "C-10", items, "CASH")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("unavailable") ||
                   ex.getMessage().toLowerCase().contains("available"));
    }
}
