package com.edstem.product_catalog.repository;

import com.edstem.product_catalog.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
