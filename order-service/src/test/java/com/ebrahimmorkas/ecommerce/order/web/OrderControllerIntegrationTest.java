package com.ebrahimmorkas.ecommerce.order.web;

import com.ebrahimmorkas.ecommerce.order.TestcontainersConfiguration;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class OrderControllerIntegrationTest {

    @RegisterExtension
    static WireMockExtension inventory = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    /** Point the load balancer at WireMock instead of Eureka. */
    @DynamicPropertySource
    static void inventoryInstance(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.inventory-service[0].uri", inventory::baseUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void placeOrderPersistsPendingOrderAndCanBeFetched() throws Exception {
        inventory.stubFor(WireMock.get(urlPathEqualTo("/api/products")).willReturn(okJson("""
                [{"skuCode": "IPHONE-15", "name": "iPhone 15", "price": 799.00, "quantityAvailable": 50}]
                """)));

        String location = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail": "jane@example.com", "items": [{"skuCode": "IPHONE-15", "quantity": 2}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("http://localhost/api/orders/")))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(1598.00))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)));

        mockMvc.perform(get("/api/orders")
                        .param("customerEmail", "JANE@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].customerEmail").value("jane@example.com"));
    }

    @Test
    void unknownSkuIsRejected() throws Exception {
        inventory.stubFor(WireMock.get(urlPathEqualTo("/api/products")).willReturn(okJson("[]")));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail": "jane@example.com", "items": [{"skuCode": "NOPE", "quantity": 1}]}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Unknown product"));
    }

    @Test
    void inventoryFailureTriggersCircuitBreakerFallback() throws Exception {
        inventory.stubFor(WireMock.get(urlPathEqualTo("/api/products")).willReturn(aResponse().withStatus(500)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail": "jane@example.com", "items": [{"skuCode": "IPHONE-15", "quantity": 1}]}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Dependency unavailable"));
    }

    @Test
    void invalidOrderIsRejected() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerEmail": "not-an-email", "items": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.customerEmail").exists())
                .andExpect(jsonPath("$.errors.items").exists());
    }
}
