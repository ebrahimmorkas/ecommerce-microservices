package com.ebrahimmorkas.ecommerce.gateway;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class GatewayRoutingIntegrationTest {

    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @RegisterExtension
    static WireMockExtension inventory = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    @RegisterExtension
    static WireMockExtension orders = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    /** Resolve lb:// service names to the WireMock servers instead of Eureka. */
    @DynamicPropertySource
    static void serviceInstances(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.inventory-service[0].uri", inventory::baseUrl);
        registry.add("spring.cloud.discovery.client.simple.instances.order-service[0].uri", orders::baseUrl);
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void routesProductRequestsToInventoryAndAddsCorrelationId() {
        inventory.stubFor(get(urlEqualTo("/api/products/PIXEL-9")).willReturn(okJson("""
                {"skuCode": "PIXEL-9"}
                """)));

        webTestClient.get().uri("/api/products/PIXEL-9")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches("X-Correlation-Id", UUID_PATTERN)
                .expectBody().jsonPath("$.skuCode").isEqualTo("PIXEL-9");

        inventory.verify(getRequestedFor(urlEqualTo("/api/products/PIXEL-9"))
                .withHeader("X-Correlation-Id", matching(UUID_PATTERN)));
    }

    @Test
    void propagatesExistingCorrelationIdToDownstreamService() {
        orders.stubFor(get(urlPathEqualTo("/api/orders")).willReturn(okJson("{}")));

        webTestClient.get().uri("/api/orders?customerEmail=jane@example.com")
                .header("X-Correlation-Id", "abc-123")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Correlation-Id", "abc-123");

        orders.verify(getRequestedFor(urlPathEqualTo("/api/orders"))
                .withQueryParam("customerEmail", equalTo("jane@example.com"))
                .withHeader("X-Correlation-Id", equalTo("abc-123")));
    }

    @Test
    void rewritesAggregatedApiDocsPathToServiceSpec() {
        inventory.stubFor(get(urlEqualTo("/v3/api-docs")).willReturn(okJson("""
                {"openapi": "3.0.1"}
                """)));

        webTestClient.get().uri("/aggregate/inventory-service/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.openapi").isEqualTo("3.0.1");
    }

    @Test
    void unknownRouteReturns404() {
        webTestClient.get().uri("/api/unknown").exchange().expectStatus().isNotFound();
    }
}
