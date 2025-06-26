package com.edstem.product_catalog.repository;

import com.edstem.product_catalog.document.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {

    List<ProductDocument> findByNameContainingIgnoreCase(String name);

    List<ProductDocument> findByCategory(String category);

    List<ProductDocument> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    List<ProductDocument> findByStockGreaterThan(Integer stock);

    @Query("{\"bool\": {\"should\": [{\"fuzzy\": {\"name\": {\"value\": \"?0\", \"fuzziness\": \"AUTO\"}}}, {\"fuzzy\": {\"description\": {\"value\": \"?0\", \"fuzziness\": \"AUTO\"}}}]}}")
    List<ProductDocument> findByNameOrDescriptionFuzzy(String searchTerm);

    @Query("{\"bool\": {\"must\": [{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"name^2\", \"description\"], \"type\": \"best_fields\", \"fuzziness\": \"AUTO\"}}]}}")
    Page<ProductDocument> findByMultiMatch(String searchTerm, Pageable pageable);

    List<ProductDocument> findTop10ByOrderByViewCountDesc();
}