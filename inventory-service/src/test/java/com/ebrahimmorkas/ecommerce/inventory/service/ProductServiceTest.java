package com.ebrahimmorkas.ecommerce.inventory.service;

import com.ebrahimmorkas.ecommerce.inventory.domain.Product;
import com.ebrahimmorkas.ecommerce.inventory.dto.ProductRequest;
import com.ebrahimmorkas.ecommerce.inventory.exception.DuplicateSkuException;
import com.ebrahimmorkas.ecommerce.inventory.exception.ProductNotFoundException;
import com.ebrahimmorkas.ecommerce.inventory.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createRejectsDuplicateSku() {
        given(productRepository.existsBySkuCode("SKU-1")).willReturn(true);

        assertThatThrownBy(() -> productService.create(
                new ProductRequest("SKU-1", "Phone", null, new BigDecimal("10.00"), 1)))
                .isInstanceOf(DuplicateSkuException.class);
        then(productRepository).should(never()).save(any());
    }

    @Test
    void adjustStockWithNegativeDeltaDecreasesStock() {
        Product product = new Product("SKU-1", "Phone", null, new BigDecimal("10.00"), 10);
        given(productRepository.findBySkuCode("SKU-1")).willReturn(Optional.of(product));

        var response = productService.adjustStock("SKU-1", -4);

        assertThat(response.quantityAvailable()).isEqualTo(6);
    }

    @Test
    void findUnknownSkuThrowsNotFound() {
        given(productRepository.findBySkuCode("MISSING")).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findBySkuCode("MISSING"))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
