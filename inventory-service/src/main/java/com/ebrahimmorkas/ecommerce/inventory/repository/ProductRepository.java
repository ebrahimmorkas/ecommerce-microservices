package com.ebrahimmorkas.ecommerce.inventory.repository;

import com.ebrahimmorkas.ecommerce.inventory.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySkuCode(String skuCode);

    List<Product> findBySkuCodeIn(Collection<String> skuCodes);

    boolean existsBySkuCode(String skuCode);
}
