package com.edstem.product_catalog.service;

import com.edstem.product_catalog.document.ProductDocument;
import com.edstem.product_catalog.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public void indexProduct(Product product) {
        log.info("Indexing product with ID: {}", product.getId());
        elasticsearchOperations.save(product);
    }

    public void updateProduct(Product product) {
        log.info("Updating product with ID: {}", product.getId());
        ProductDocument doc = ProductDocument.fromProduct(product);
        elasticsearchOperations.save(doc);
    }

    public void deleteProduct(String id) {
        log.info("Deleting product with ID: {}", id);
        elasticsearchOperations.delete(id, ProductDocument.class);
    }

    public void reindexAllProducts(List<Product> products) {
        log.info("Reindexing {} products...", products.size());
        elasticsearchOperations.save(products);
        log.info("Reindex completed.");
    }

    public List<ProductDocument> searchProducts(String q) {
        if (q == null || q.isBlank()) {
            return findAll();
        }

        Query query = NativeQuery.builder()
                .withQuery(qb -> qb
                        .queryString(qs -> qs
                                .query(q)
                                .fields("name", "description")))
                .build();

        return executeSearch(query);
    }

    public List<ProductDocument> searchProductsByName(String name) {
        Query query = NativeQuery.builder()
                .withQuery(q -> q.match(m -> m
                        .field("name")
                        .query(name)))
                .build();

        return executeSearch(query);
    }

    public List<ProductDocument> searchProductsByCategory(String category) {
        Query query = NativeQuery.builder()
                .withQuery(q -> q.match(m -> m
                        .field("category")
                        .query(category)))
                .build();

        return executeSearch(query);
    }

    public List<ProductDocument> searchProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        String rangeQuery = "price:[" + minPrice + " TO " + maxPrice + "]";

        Query query = NativeQuery.builder()
                .withQuery(q -> q.queryString(qs -> qs
                        .query(rangeQuery)))
                .build();

        return executeSearch(query);
    }

    public List<ProductDocument> fuzzySearch(String q) {
        Query query = NativeQuery.builder()
                .withQuery(qb -> qb
                        .fuzzy(f -> f
                                .field("name")
                                .value(q)
                                .fuzziness("AUTO")))
                .build();

        return executeSearch(query);
    }

    public List<ProductDocument> advancedSearch(String name, BigDecimal minPrice) {
        StringBuilder queryBuilder = new StringBuilder();

        if (name != null && !name.isBlank()) {
            queryBuilder.append("name:").append(name);
        }

        if (minPrice != null) {
            if (queryBuilder.length() > 0) queryBuilder.append(" AND ");
            queryBuilder.append("price:[").append(minPrice).append(" TO *]");
        }

        Query query = NativeQuery.builder()
                .withQuery(q -> q.queryString(qs -> qs
                        .query(queryBuilder.toString())))
                .build();

        return executeSearch(query);
    }

    public Page<ProductDocument> searchProductsWithPagination(String q, Pageable pageable) {
        Query query = NativeQuery.builder()
                .withQuery(qb -> qb
                        .queryString(qs -> qs
                                .query(q)
                                .fields("name", "description")))
                .withPageable(pageable)
                .build();

        SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);

        List<ProductDocument> content = hits.getSearchHits()
                .stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, hits.getTotalHits());
    }

    public List<ProductDocument> findAll() {
        Query query = NativeQuery.builder()
                .withQuery(q -> q.matchAll(m -> m))
                .build();

        return executeSearch(query);
    }

    private List<ProductDocument> executeSearch(Query query) {
        SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);
        return hits.getSearchHits()
                .stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }
}