package com.edstem.product_catalog.controller;

import com.edstem.product_catalog.document.ProductDocument;
import com.edstem.product_catalog.service.SearchService;
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

    private final SearchService searchService;

    @GetMapping("/products")
    public ResponseEntity<List<ProductDocument>> searchProducts(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(searchService.searchProducts(q));
    }

    @GetMapping("/products/name")
    public ResponseEntity<List<ProductDocument>> searchProductsByName(@RequestParam String name) {
        return ResponseEntity.ok(searchService.searchByName(name));
    }

    @GetMapping("/products/category")
    public ResponseEntity<List<ProductDocument>> searchProductsByCategory(@RequestParam String category) {
        return ResponseEntity.ok(searchService.searchByCategory(category));
    }

    @GetMapping("/products/price")
    public ResponseEntity<List<ProductDocument>> searchProductsByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice) {
        return ResponseEntity.ok(searchService.searchByPriceRange(minPrice, maxPrice));
    }

    @GetMapping("/products/fuzzy")
    public ResponseEntity<List<ProductDocument>> fuzzySearchProducts(@RequestParam String q) {
        return ResponseEntity.ok(searchService.fuzzySearch(q));
    }

    @GetMapping("/products/advanced")
    public ResponseEntity<List<ProductDocument>> advancedSearchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) BigDecimal minPrice) {
        return ResponseEntity.ok(searchService.advancedSearch(name, minPrice));
    }

    @GetMapping("/products/paginated")
    public ResponseEntity<Page<ProductDocument>> searchProductsWithPagination(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(searchService.paginatedSearch(q, pageable));
    }

    @PostMapping("/reindex")
    public ResponseEntity<String> reindexAllProducts() {
        searchService.reindexAll();
        return ResponseEntity.ok("Reindexing completed successfully");
    }
}
