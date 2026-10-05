package com.shopflow.order.domain;

/**
 * Siklus hidup pesanan.
 *
 * <pre>
 *  PENDING_PAYMENT --(payment.completed)--> PAID --> FULFILLED
 *         |                                    |
 *         |--(payment.failed)--> PAYMENT_FAILED
 *         |--(user batal)------> CANCELLED
 * </pre>
 */
public enum OrderStatus {

    PENDING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    CANCELLED,
    FULFILLED;

    public boolean isTerminal() {
        return this == CANCELLED || this == FULFILLED;
    }
}
