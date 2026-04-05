package com.stadiumeats.service;

import java.math.BigDecimal;

/**
 * OnlinePaymentStrategy — concrete Strategy for card / online payment.
 * In a real system this would call a payment gateway.
 */
public class OnlinePaymentStrategy implements PaymentStrategy {

    @Override
    public String processPayment(BigDecimal amount) {
        // Real impl: call Stripe / PayPal / etc.
        // Here we simulate a successful payment.
        return getMethodName();
    }

    @Override
    public String getMethodName() {
        return "ONLINE";
    }
}
