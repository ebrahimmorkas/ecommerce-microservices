package com.ebrahimmorkas.ecommerce.inventory.domain;

public enum ReservationStatus {
    /** Stock was deducted for the order. */
    RESERVED,
    /** Stock was returned because the order failed downstream (e.g. payment declined). */
    RELEASED,
    /** Stock could not be reserved; recorded so a redelivered event is not re-evaluated. */
    REJECTED
}
