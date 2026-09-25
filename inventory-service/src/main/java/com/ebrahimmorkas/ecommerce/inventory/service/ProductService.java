package com.ebrahimmorkas.ecommerce.inventory.service;

import com.ebrahimmorkas.ecommerce.inventory.domain.Product;
import com.ebrahimmorkas.ecommerce.inventory.dto.ProductRequest;
import com.ebrahimmorkas.ecommerce.inventory.dto.ProductResponse;
import com.ebrahimmorkas.ecommerce.inventory.exception.DuplicateSkuException;
import com.ebrahimmorkas.ecommerce.inventory.exception.ProductNotFoundException;
import com.ebrahimmorkas.ecommerce.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public Page<ProductResponse> findAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(ProductResponse::from);
    }

    public List<ProductResponse> findBySkuCodes(Collection<String> skuCodes) {
        return productRepository.findBySkuCodeIn(skuCodes).stream().map(ProductResponse::from).toList();
    }

    public ProductResponse findBySkuCode(String skuCode) {
        return ProductResponse.from(getProduct(skuCode));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySkuCode(request.skuCode())) {
            throw new DuplicateSkuException(request.skuCode());
        }
        Product product = new Product(request.skuCode(), request.name(), request.description(),
                request.price(), request.quantityAvailable());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse adjustStock(String skuCode, int delta) {
        Product product = getProduct(skuCode);
        if (delta >= 0) {
            product.increaseStock(delta);
        } else {
            product.decreaseStock(-delta);
        }
        return ProductResponse.from(product);
    }

    private Product getProduct(String skuCode) {
        return productRepository.findBySkuCode(skuCode).orElseThrow(() -> new ProductNotFoundException(skuCode));
    }
}
