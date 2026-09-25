package com.ebrahimmorkas.ecommerce.inventory.web;

import com.ebrahimmorkas.ecommerce.inventory.dto.ProductRequest;
import com.ebrahimmorkas.ecommerce.inventory.dto.ProductResponse;
import com.ebrahimmorkas.ecommerce.inventory.dto.StockAdjustmentRequest;
import com.ebrahimmorkas.ecommerce.inventory.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog and stock levels")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "List products (paginated)")
    public Page<ProductResponse> findAll(@PageableDefault(size = 20, sort = "skuCode") Pageable pageable) {
        return productService.findAll(pageable);
    }

    @GetMapping(params = "skuCodes")
    @Operation(summary = "Batch lookup of products by SKU codes")
    public List<ProductResponse> findBySkuCodes(@RequestParam List<String> skuCodes) {
        return productService.findBySkuCodes(skuCodes);
    }

    @GetMapping("/{skuCode}")
    @Operation(summary = "Get a product by SKU code")
    public ProductResponse findBySkuCode(@PathVariable String skuCode) {
        return productService.findBySkuCode(skuCode);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a product")
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PatchMapping("/{skuCode}/stock")
    @Operation(summary = "Adjust stock level by a positive or negative delta")
    public ProductResponse adjustStock(@PathVariable String skuCode, @Valid @RequestBody StockAdjustmentRequest request) {
        return productService.adjustStock(skuCode, request.delta());
    }
}
