package com.edstem.product_catalog.controller;

import com.edstem.product_catalog.document.ProductDocument;
import com.edstem.product_catalog.service.ElasticsearchService;
import com.edstem.product_catalog.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final ElasticsearchService elasticsearchService;
    private final ProductService productService;

    @GetMapping("/products")
    public ResponseEntity<List<ProductDocument>> searchProducts(@RequestParam(required = false) String q) {
        log.info("Searching products with query: {}", q);
        try {
            List<ProductDocument> products = elasticsearchService.searchProducts(q);
            log.info("Found {} products", products.size());
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error searching products", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/products/name")
    public ResponseEntity<List<ProductDocument>> searchProductsByName(@RequestParam String name) {
        log.info("Searching products by name: {}", name);
        try {
            return ResponseEntity.ok(elasticsearchService.searchProductsByName(name));
        } catch (Exception e) {
            log.error("Error searching products by name", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/products/category")
    public ResponseEntity<List<ProductDocument>> searchProductsByCategory(@RequestParam String category) {
        log.info("Searching products by category: {}", category);
        try {
            return ResponseEntity.ok(elasticsearchService.searchProductsByCategory(category));
        } catch (Exception e) {
            log.error("Error searching products by category", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/products/price")
    public ResponseEntity<List<ProductDocument>> searchProductsByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice) {
        log.info("Searching products by price range: {} - {}", minPrice, maxPrice);
        try {
            return ResponseEntity.ok(elasticsearchService.searchProductsByPriceRange(minPrice, maxPrice));
        } catch (Exception e) {
            log.error("Error searching products by price range", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/products/fuzzy")
    public ResponseEntity<List<ProductDocument>> fuzzySearchProducts(@RequestParam String q) {
        log.info("Fuzzy searching products with query: {}", q);
        try {
            return ResponseEntity.ok(elasticsearchService.fuzzySearch(q));
        } catch (Exception e) {
            log.error("Error in fuzzy search", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/products/advanced")
    public ResponseEntity<List<ProductDocument>> advancedSearchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) BigDecimal minPrice) {
        log.info("Advanced searching products with name: {} and minPrice: {}", name, minPrice);
        try {
            return ResponseEntity.ok(elasticsearchService.advancedSearch(name, minPrice));
        } catch (Exception e) {
            log.error("Error in advanced search", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/products/paginated")
    public ResponseEntity<Page<ProductDocument>> searchProductsWithPagination(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Paginated search for products with query: {}, page: {}, size: {}", q, page, size);
        try {
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(elasticsearchService.searchProductsWithPagination(q, pageable));
        } catch (Exception e) {
            log.error("Error in paginated search", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/reindex")
    public ResponseEntity<String> reindexAllProducts() {
        log.info("Reindexing all products in Elasticsearch");
        try {
            productService.reindexAllProducts();
            return ResponseEntity.ok("Reindexing completed successfully");
        } catch (Exception e) {
            log.error("Error reindexing products", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
