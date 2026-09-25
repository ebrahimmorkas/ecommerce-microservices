package com.ebrahimmorkas.ecommerce.inventory.web;

import com.ebrahimmorkas.ecommerce.inventory.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void seededProductsAreListedWithPagination() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page.totalElements").isNumber());
    }

    @Test
    void createThenFetchProduct() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"skuCode": "KINDLE-11", "name": "Kindle", "price": 99.99, "quantityAvailable": 7}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.skuCode").value("KINDLE-11"));

        mockMvc.perform(get("/api/products/KINDLE-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityAvailable").value(7));
    }

    @Test
    void creatingDuplicateSkuReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"skuCode": "IPHONE-15", "name": "Dup", "price": 1.00, "quantityAvailable": 1}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate SKU"));
    }

    @Test
    void invalidRequestReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"skuCode": "", "name": "X", "price": 0, "quantityAvailable": -1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.skuCode").exists())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    @Test
    void removingMoreStockThanAvailableReturns422() throws Exception {
        mockMvc.perform(patch("/api/products/MACBOOK-AIR/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"delta": -1000}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void batchLookupBySkuCodes() throws Exception {
        mockMvc.perform(get("/api/products").param("skuCodes", "PIXEL-9", "AIRPODS-PRO", "UNKNOWN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void unknownProductReturns404() throws Exception {
        mockMvc.perform(get("/api/products/NOPE"))
                .andExpect(status().isNotFound());
    }
}
