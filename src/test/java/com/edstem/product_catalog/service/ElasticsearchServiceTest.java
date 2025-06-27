package com.edstem.product_catalog.service;

import com.edstem.product_catalog.document.ProductDocument;
import com.edstem.product_catalog.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.query.Query;


import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElasticsearchServiceTest {

    @Mock private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks private ElasticsearchService elasticsearchService;

    @Mock private SearchHits<ProductDocument> searchHits;
    @Mock private SearchHit<ProductDocument> searchHit;

    private Product product;
    private ProductDocument doc;

    @BeforeEach
    void setUp() {
        product = Product.builder().id(1L).name("Test Product").build();

        doc = ProductDocument.builder()
                .id("1")
                .name("Test Product")
                .category("Electronics")
                .price(BigDecimal.valueOf(99.99))
                .stock(10)
                .build();
    }

    @Test
    void indexProduct_ShouldCallSave() {
        elasticsearchService.indexProduct(product);
        verify(elasticsearchOperations).save(product);
    }

    @Test
    void updateProduct_ShouldConvertAndSaveDocument() {
        try (MockedStatic<ProductDocument> mocked = mockStatic(ProductDocument.class)) {
            mocked.when(() -> ProductDocument.fromProduct(product)).thenReturn(doc);

            elasticsearchService.updateProduct(product);

            verify(elasticsearchOperations).save(doc);
        }
    }

    @Test
    void deleteProduct_ShouldCallElasticsearchDelete() {
        elasticsearchService.deleteProduct("1");
        verify(elasticsearchOperations).delete("1", ProductDocument.class);
    }

    @Test
    void reindexAllProducts_ShouldCallSaveWithList() {
        List<Product> products = List.of(product);
        elasticsearchService.reindexAllProducts(products);
        verify(elasticsearchOperations).save(products);
    }

    @Test
    void searchProducts_ShouldReturnMatchingDocuments() {
        stubSearchReturningHits(List.of(doc));

        List<ProductDocument> result = elasticsearchService.searchProducts("query");

        assertEquals(1, result.size());
        assertEquals("Test Product", result.get(0).getName());
    }

    @Test
    void fuzzySearch_ShouldReturnFuzzyMatches() {
        stubSearchReturningHits(List.of(doc));

        List<ProductDocument> result = elasticsearchService.fuzzySearch("tst");

        assertEquals(1, result.size());
    }

    @Test
    void searchProductsByName_ShouldReturnMatches() {
        stubSearchReturningHits(List.of(doc));

        List<ProductDocument> result = elasticsearchService.searchProductsByName("Test");

        assertEquals(1, result.size());
    }

    @Test
    void searchProductsByCategory_ShouldReturnMatches() {
        stubSearchReturningHits(List.of(doc));

        List<ProductDocument> result = elasticsearchService.searchProductsByCategory("Electronics");

        assertEquals(1, result.size());
    }

    @Test
    void searchProductsByPriceRange_ShouldReturnMatches() {
        stubSearchReturningHits(List.of(doc));

        List<ProductDocument> result = elasticsearchService.searchProductsByPriceRange(BigDecimal.valueOf(50), BigDecimal.valueOf(150));

        assertEquals(1, result.size());
    }

    @Test
    void advancedSearch_ShouldReturnFilteredResults() {
        stubSearchReturningHits(List.of(doc));

        List<ProductDocument> result = elasticsearchService.advancedSearch("Test", BigDecimal.valueOf(10));

        assertEquals(1, result.size());
    }

    @Test
    void searchProductsWithPagination_ShouldReturnPage() {
        when(elasticsearchOperations.search((Query) any(), eq(ProductDocument.class)))
                .thenReturn(searchHits);
        when(searchHits.getSearchHits()).thenReturn(List.of(searchHit));
        when(searchHits.getTotalHits()).thenReturn(1L);
        when(searchHit.getContent()).thenReturn(doc);

        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductDocument> result = elasticsearchService.searchProductsWithPagination("query", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(doc, result.getContent().get(0));
    }

    @Test
    void findAll_ShouldReturnAllDocuments() {
        stubSearchReturningHits(List.of(doc));

        List<ProductDocument> result = elasticsearchService.findAll();

        assertEquals(1, result.size());
    }

    private void stubSearchReturningHits(List<ProductDocument> documents) {
        List<SearchHit<ProductDocument>> searchHitList = new ArrayList<>();
        for (ProductDocument d : documents) {
            SearchHit<ProductDocument> hit = mock(SearchHit.class);
            when(hit.getContent()).thenReturn(d);
            searchHitList.add(hit);
        }

        when(elasticsearchOperations.search((Query) any(), eq(ProductDocument.class)))
                .thenReturn(searchHits);
        when(searchHits.getSearchHits()).thenReturn(searchHitList);
    }
}
