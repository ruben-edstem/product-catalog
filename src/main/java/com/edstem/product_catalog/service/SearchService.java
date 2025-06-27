package com.edstem.product_catalog.service;

import com.edstem.product_catalog.document.ProductDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchService elasticsearchService;
    private final ProductService productService;

    public List<ProductDocument> searchProducts(String q) {
        return elasticsearchService.searchProducts(q);
    }

    public List<ProductDocument> searchByName(String name) {
        return elasticsearchService.searchProductsByName(name);
    }

    public List<ProductDocument> searchByCategory(String category) {
        return elasticsearchService.searchProductsByCategory(category);
    }

    public List<ProductDocument> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return elasticsearchService.searchProductsByPriceRange(minPrice, maxPrice);
    }

    public List<ProductDocument> fuzzySearch(String q) {
        return elasticsearchService.fuzzySearch(q);
    }

    public List<ProductDocument> advancedSearch(String name, BigDecimal minPrice) {
        return elasticsearchService.advancedSearch(name, minPrice);
    }

    public Page<ProductDocument> paginatedSearch(String q, Pageable pageable) {
        return elasticsearchService.searchProductsWithPagination(q, pageable);
    }

    public void reindexAll() {
        productService.reindexAllProducts();
    }
}
