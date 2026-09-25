package com.ebrahimmorkas.ecommerce.order.client;

import com.ebrahimmorkas.ecommerce.order.exception.InventoryUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.List;

/**
 * Synchronous client for inventory-service. Calls go through a Resilience4j circuit breaker so a
 * slow or failing inventory service fails fast instead of exhausting order-service threads.
 */
@Slf4j
@Component
public class InventoryClient {

    private static final ParameterizedTypeReference<List<ProductInfo>> PRODUCT_LIST = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    public InventoryClient(RestClient.Builder loadBalancedBuilder,
                           CircuitBreakerFactory<?, ?> circuitBreakerFactory,
                           @Value("${clients.inventory.base-url}") String baseUrl) {
        this.restClient = loadBalancedBuilder.baseUrl(baseUrl).build();
        this.circuitBreaker = circuitBreakerFactory.create("inventory");
    }

    public List<ProductInfo> findProducts(Collection<String> skuCodes) {
        return circuitBreaker.run(
                () -> restClient.get()
                        .uri(uri -> uri.path("/api/products").queryParam("skuCodes", skuCodes).build())
                        .retrieve()
                        .body(PRODUCT_LIST),
                failure -> {
                    log.warn("Inventory lookup failed for {}: {}", skuCodes, failure.toString());
                    throw new InventoryUnavailableException(failure);
                });
    }
}
