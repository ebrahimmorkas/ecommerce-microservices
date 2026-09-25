package com.ebrahimmorkas.ecommerce.payment.web;

import com.ebrahimmorkas.ecommerce.payment.dto.PaymentResponse;
import com.ebrahimmorkas.ecommerce.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment status per order")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Get the payment for an order")
    public PaymentResponse findByOrderNumber(@PathVariable UUID orderNumber) {
        return paymentService.findByOrderNumber(orderNumber);
    }
}
