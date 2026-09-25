package com.ebrahimmorkas.ecommerce.order.exception;

import java.util.Collection;

public class UnknownProductException extends RuntimeException {

    public UnknownProductException(Collection<String> skuCodes) {
        super("Unknown product SKU(s): " + String.join(", ", skuCodes));
    }
}
