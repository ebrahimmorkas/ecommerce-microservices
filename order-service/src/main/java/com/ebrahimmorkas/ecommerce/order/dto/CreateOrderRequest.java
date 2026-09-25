package com.ebrahimmorkas.ecommerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank @Email String customerEmail,
        @NotEmpty @Size(max = 50) List<@Valid Item> items) {

    public record Item(
            @NotBlank String skuCode,
            @Min(1) @Max(100) int quantity) {
    }
}
