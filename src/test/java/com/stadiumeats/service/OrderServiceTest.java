package com.stadiumeats.service;

import com.stadiumeats.dao.MenuItemDAO;
import com.stadiumeats.dao.OrderDAO;
import com.stadiumeats.model.MenuItem;
import com.stadiumeats.model.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderService — uses Mockito to mock OrderDAO and MenuItemDAO.
 * No real database connection required.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderDAO orderDAO;

    @Mock
    private MenuItemDAO menuItemDAO;

    @InjectMocks
    private OrderService orderService;

    // ─── helpers ─────────────────────────────────────────────────

    private MenuItem menuItem(long id, String name, double price) {
        MenuItem m = new MenuItem();
        m.setId(id);
        m.setName(name);
        m.setPrice(BigDecimal.valueOf(price));
        m.setAvailable(true);
        return m;
    }

    private Map<String, Object> cartItem(int menuItemId, int qty) {
        Map<String, Object> item = new HashMap<>();
        item.put("menuItemId", menuItemId);
        item.put("quantity",   qty);
        return item;
    }

    // ─── placeOrder ──────────────────────────────────────────────

    @Test
    void testPlaceOrderSuccessCash() throws SQLException {
        // Arrange
        MenuItem burger = menuItem(1L, "Classic Burger", 12.99);
        when(menuItemDAO.findById(1L)).thenReturn(Optional.of(burger));
        when(orderDAO.saveWithItems(any(Order.class), anyList())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(100L);
            return o;
        });

        // Act
        Order result = orderService.placeOrder(
                1L, "A-12", List.of(cartItem(1, 2)), "CASH");

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("CASH", result.getPaymentMethod());
        // 12.99 × 2 = 25.98
        assertEquals(0, new BigDecimal("25.98").compareTo(result.getTotalPrice()));
        verify(orderDAO).saveWithItems(any(Order.class), anyList());
    }

    @Test
    void testPlaceOrderSuccessOnline() throws SQLException {
        // Arrange
        MenuItem pizza = menuItem(2L, "Pizza Slice", 8.99);
        when(menuItemDAO.findById(2L)).thenReturn(Optional.of(pizza));
        when(orderDAO.saveWithItems(any(Order.class), anyList())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(101L);
            return o;
        });

        // Act
        Order result = orderService.placeOrder(
                2L, "B-05", List.of(cartItem(2, 1)), "ONLINE");

        // Assert
        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals("ONLINE", result.getPaymentMethod());
        assertEquals(0, new BigDecimal("8.99").compareTo(result.getTotalPrice()));
    }

    @Test
    void testPlaceOrderInvalidMenuItem() throws SQLException {
        // Arrange: item 999 not in DB
        when(menuItemDAO.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.placeOrder(1L, "A-12", List.of(cartItem(999, 1)), "CASH"));
        assertTrue(ex.getMessage().contains("999"));
        verify(orderDAO, never()).saveWithItems(any(), anyList());
    }

    @Test
    void testPlaceOrderEmptyCart() {
        // Act + Assert — no DB call needed
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.placeOrder(1L, "A-12", Collections.emptyList(), "CASH"));
        assertEquals("Cart is empty", ex.getMessage());
    }

    @Test
    void testPlaceOrderMissingSeatNumber() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.placeOrder(1L, "  ", List.of(cartItem(1, 1)), "CASH"));
        assertEquals("Seat number is required", ex.getMessage());
    }

    // ─── updateStatus ────────────────────────────────────────────

    @Test
    void testUpdateOrderStatus() throws SQLException {
        // Arrange
        Order existing = new Order();
        existing.setId(1L);
        existing.setStatus("PENDING");
        when(orderDAO.findById(1L)).thenReturn(Optional.of(existing));
        doNothing().when(orderDAO).updateStatus(1L, "IN_DELIVERY");

        // Act + Assert: no exception thrown
        assertDoesNotThrow(() -> orderService.updateStatus(1L, "IN_DELIVERY"));
        verify(orderDAO).updateStatus(eq(1L), eq("IN_DELIVERY"));
    }

    @Test
    void testUpdateOrderStatusInvalidValue() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.updateStatus(1L, "FLYING"));
        assertTrue(ex.getMessage().contains("Invalid status"));
    }

    @Test
    void testUpdateOrderStatusNotFound() throws SQLException {
        when(orderDAO.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderService.updateStatus(999L, "DELIVERED"));
        assertTrue(ex.getMessage().contains("999"));
    }
}
