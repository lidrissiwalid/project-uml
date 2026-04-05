package com.stadiumeats.service;

/**
 * PaymentStrategy — Strategy pattern interface.
 * Open/Closed Principle: add new payment methods by creating a new impl,
 * never modifying OrderService.
 */
public interface PaymentStrategy {

    /**
     * Process a payment.
     * @param amount the amount to charge (for logging/gateway calls)
     * @return the payment method label stored in the order
     */
    String processPayment(java.math.BigDecimal amount);

    /** Human-readable method name returned in API responses. */
    String getMethodName();
}
