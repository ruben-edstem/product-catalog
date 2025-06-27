package com.edstem.product_catalog.service;

import com.edstem.product_catalog.document.ProductDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private ElasticsearchService elasticsearchService;

    @Mock
    private ProductService productService;

    @InjectMocks
    private SearchService searchService;

    private ProductDocument testProduct;

    @BeforeEach
    void setUp() {
        testProduct = ProductDocument.builder()
                .id("1")
                .name("Test Product")
                .description("Test Description")
                .category("Test Category")
                .price(BigDecimal.valueOf(100))
                .stock(10)
                .build();
    }

    @Test
    void searchProducts_ShouldReturnMatchingProducts() {
        when(elasticsearchService.searchProducts("test")).thenReturn(List.of(testProduct));

        List<ProductDocument> result = searchService.searchProducts("test");

        assertEquals(1, result.size());
        assertEquals("Test Product", result.get(0).getName());
        verify(elasticsearchService).searchProducts("test");
    }

    @Test
    void searchByName_ShouldReturnProductsByName() {
        when(elasticsearchService.searchProductsByName("Test Product")).thenReturn(List.of(testProduct));

        List<ProductDocument> result = searchService.searchByName("Test Product");

        assertEquals(1, result.size());
        verify(elasticsearchService).searchProductsByName("Test Product");
    }

    @Test
    void searchByCategory_ShouldReturnProductsByCategory() {
        when(elasticsearchService.searchProductsByCategory("Test Category")).thenReturn(List.of(testProduct));

        List<ProductDocument> result = searchService.searchByCategory("Test Category");

        assertEquals(1, result.size());
        verify(elasticsearchService).searchProductsByCategory("Test Category");
    }

    @Test
    void searchByPriceRange_ShouldReturnProductsInRange() {
        BigDecimal min = BigDecimal.valueOf(50);
        BigDecimal max = BigDecimal.valueOf(150);
        when(elasticsearchService.searchProductsByPriceRange(min, max)).thenReturn(List.of(testProduct));

        List<ProductDocument> result = searchService.searchByPriceRange(min, max);

        assertEquals(1, result.size());
        verify(elasticsearchService).searchProductsByPriceRange(min, max);
    }

    @Test
    void fuzzySearch_ShouldReturnFuzzyMatches() {
        when(elasticsearchService.fuzzySearch("tst")).thenReturn(List.of(testProduct));

        List<ProductDocument> result = searchService.fuzzySearch("tst");

        assertEquals(1, result.size());
        verify(elasticsearchService).fuzzySearch("tst");
    }

    @Test
    void advancedSearch_ShouldReturnResults() {
        when(elasticsearchService.advancedSearch("Test Product", BigDecimal.valueOf(100)))
                .thenReturn(List.of(testProduct));

        List<ProductDocument> result = searchService.advancedSearch("Test Product", BigDecimal.valueOf(100));

        assertEquals(1, result.size());
        verify(elasticsearchService).advancedSearch("Test Product", BigDecimal.valueOf(100));
    }

    @Test
    void paginatedSearch_ShouldReturnPagedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductDocument> page = new PageImpl<>(List.of(testProduct), pageable, 1);

        when(elasticsearchService.searchProductsWithPagination("test", pageable)).thenReturn(page);

        Page<ProductDocument> result = searchService.paginatedSearch("test", pageable);

        assertEquals(1, result.getContent().size());
        verify(elasticsearchService).searchProductsWithPagination("test", pageable);
    }

    @Test
    void reindexAll_ShouldInvokeProductService() {
        doNothing().when(productService).reindexAllProducts();

        searchService.reindexAll();

        verify(productService).reindexAllProducts();
    }
}