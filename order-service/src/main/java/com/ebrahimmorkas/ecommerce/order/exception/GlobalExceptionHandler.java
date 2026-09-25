package com.ebrahimmorkas.ecommerce.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/** Maps domain exceptions to RFC 9457 problem details. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail handleNotFound(OrderNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Order not found", ex.getMessage());
    }

    @ExceptionHandler(UnknownProductException.class)
    ProblemDetail handleUnknownProduct(UnknownProductException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Unknown product", ex.getMessage());
    }

    @ExceptionHandler(InventoryUnavailableException.class)
    ProblemDetail handleInventoryUnavailable(InventoryUnavailableException ex) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Dependency unavailable", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed", "Request body is invalid");
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, e -> String.valueOf(e.getDefaultMessage()), (a, b) -> a));
        problem.setProperty("errors", errors);
        return problem;
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
