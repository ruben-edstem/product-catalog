package com.edstem.product_catalog.controller;

import com.edstem.product_catalog.kafka.ProductProducer;
import com.edstem.product_catalog.model.Product;
import com.edstem.product_catalog.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final ProductProducer productProducer;

    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        log.info("Creating new product: {}", product.getName());
        try {
            Product createdProduct = productService.createProduct(product);
            log.info("Product created successfully with id: {}", createdProduct.getId());
            return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error creating product: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        log.info("Fetching product with id: {}", id);
        try {
            Product product = productService.getProductById(id);
            if (product != null) {
                log.info("Product found with id: {}", id);
                return new ResponseEntity<>(product, HttpStatus.OK);
            } else {
                log.warn("Product not found with id: {}", id);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            log.error("Error fetching product with id {}: {}", id, e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        log.info("Fetching all products");
        try {
            List<Product> products = productService.getAllProducts();
            log.info("Retrieved {} products", products.size());
            return new ResponseEntity<>(products, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error fetching all products: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @Valid @RequestBody Product productDetails) {
        log.info("Updating product with id: {}", id);
        try {
            Product updatedProduct = productService.updateProduct(id, productDetails);
            if (updatedProduct != null) {
                log.info("Product updated successfully with id: {}", id);
                return new ResponseEntity<>(updatedProduct, HttpStatus.OK);
            } else {
                log.warn("Product not found for update with id: {}", id);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            log.error("Error updating product with id {}: {}", id, e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.info("Deleting product with id: {}", id);
        try {
            productService.deleteProduct(id);
            log.info("Product deleted successfully with id: {}", id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            log.error("Error deleting product with id {}: {}", id, e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendProduct(@RequestBody Product product) {
        productProducer.sendProduct(product);
        return ResponseEntity.ok("Product sent to Kafka");
    }

    @PostMapping("/send-all")
    public ResponseEntity<String> sendMany() {
        productProducer.sendProducts();
        return ResponseEntity.ok("Products sent to Kafka");
    }
}

