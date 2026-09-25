package com.ebrahimmorkas.ecommerce.order.domain;

public enum OrderStatus {
    /** Order accepted; waiting for stock reservation and payment. */
    PENDING,
    /** Stock reserved and payment captured. */
    CONFIRMED,
    /** Stock unavailable or payment declined. */
    CANCELLED
}
