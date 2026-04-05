package com.stadiumeats.service;

import java.math.BigDecimal;

/**
 * CashPaymentStrategy — concrete Strategy for cash on delivery.
 */
public class CashPaymentStrategy implements PaymentStrategy {

    @Override
    public String processPayment(BigDecimal amount) {
        // Cash is collected at seat by the worker.
        return getMethodName();
    }

    @Override
    public String getMethodName() {
        return "CASH";
    }
}
