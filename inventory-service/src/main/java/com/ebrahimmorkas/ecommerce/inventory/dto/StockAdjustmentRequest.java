package com.ebrahimmorkas.ecommerce.inventory.dto;

import jakarta.validation.constraints.NotNull;

/** A positive delta restocks; a negative delta removes stock (e.g. damaged goods). */
public record StockAdjustmentRequest(@NotNull Integer delta) {
}
