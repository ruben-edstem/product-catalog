package com.edstem.product_catalog.service;

import com.edstem.product_catalog.contract.ProductDTO;
import com.edstem.product_catalog.document.ProductDocument;
import com.edstem.product_catalog.model.Product;
import com.edstem.product_catalog.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private GenericCacheService cacheService;
    @Mock private KafkaTemplate<String, ProductDTO> kafkaTemplate;
    @Mock private ElasticsearchService elasticsearchService;
    @Mock private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks private ProductService productService;

    private Product inputProduct;
    private Product savedProduct;
    private ProductDocument productDoc;

    @BeforeEach
    void setUp() {
        inputProduct = Product.builder()
                .name("Test Product")
                .description("A sample product")
                .category("Electronics")
                .price(BigDecimal.valueOf(99.99))
                .stock(10)
                .build();

        savedProduct = inputProduct.toBuilder().id(1L).build();

        productDoc = ProductDocument.builder()
                .id("1")
                .name("Test Product")
                .description("A sample product")
                .category("Electronics")
                .price(BigDecimal.valueOf(99.99))
                .stock(10)
                .build();
    }

    @Test
    void createProduct_ShouldSaveProductAndIndexToElasticsearch() {
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        try (MockedStatic<ProductDocument> mocked = mockStatic(ProductDocument.class)) {
            mocked.when(() -> ProductDocument.fromProduct(savedProduct)).thenReturn(productDoc);

            Product result = productService.createProduct(inputProduct);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            verify(productRepository).save(any(Product.class));
            verify(elasticsearchOperations).save(productDoc);
        }
    }

    @Test
    void getProductById_WhenCached_ShouldReturnCachedProduct() {
        when(cacheService.getCachedObject("product:1", Product.class)).thenReturn(savedProduct);

        Product result = productService.getProductById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(cacheService).getCachedObject("product:1", Product.class);
        verify(productRepository, never()).findById(1L);
    }

    @Test
    void getProductById_WhenNotCached_ShouldFetchFromDbAndCache() {
        when(cacheService.getCachedObject("product:1", Product.class)).thenReturn(null);
        when(productRepository.findById(1L)).thenReturn(Optional.of(savedProduct));

        Product result = productService.getProductById(1L);

        assertNotNull(result);
        verify(productRepository).findById(1L);
        verify(cacheService).cacheObject(eq("product:1"), eq(savedProduct), any(Duration.class));
    }

    @Test
    void getProductById_WhenNotFound_ShouldReturnNull() {
        when(cacheService.getCachedObject("product:1", Product.class)).thenReturn(null);
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        Product result = productService.getProductById(1L);

        assertNull(result);
    }

    @Test
    void getAllProducts_WhenCached_ShouldReturnFromCache() {
        List<Product> cached = List.of(savedProduct);
        when(cacheService.getCachedObject("products:all", List.class)).thenReturn(cached);

        List<Product> result = productService.getAllProducts();

        assertEquals(1, result.size());
        verify(productRepository, never()).findAll();
    }

    @Test
    void getAllProducts_WhenNotCached_ShouldFetchFromDbAndCache() {
        List<Product> dbList = List.of(savedProduct);
        when(cacheService.getCachedObject("products:all", List.class)).thenReturn(null);
        when(productRepository.findAll()).thenReturn(dbList);

        List<Product> result = productService.getAllProducts();

        assertEquals(1, result.size());
        verify(productRepository).findAll();
        verify(cacheService).cacheObject(eq("products:all"), eq(dbList), any(Duration.class));
        verify(cacheService).cacheObject(eq("product:1"), eq(savedProduct), any(Duration.class));
    }

    @Test
    void updateProduct_WhenFound_ShouldUpdateAndCacheAndIndex() {
        Product updated = savedProduct.toBuilder().name("Updated").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(savedProduct));
        when(productRepository.save(any())).thenReturn(updated);

        Product result = productService.updateProduct(1L, updated);

        assertEquals("Updated", result.getName());
        verify(cacheService).cacheObject(eq("product:1"), eq(updated), any(Duration.class));
        verify(cacheService).evictCache("products:all");
        verify(elasticsearchService).updateProduct(updated);
    }

    @Test
    void updateProduct_WhenNotFound_ShouldReturnNull() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        Product result = productService.updateProduct(1L, inputProduct);

        assertNull(result);
    }

    @Test
    void deleteProduct_WhenFound_ShouldDeleteAll() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(savedProduct));

        productService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
        verify(cacheService).evictCache("product:1");
        verify(cacheService).evictCache("products:all");
        verify(elasticsearchService).deleteProduct("1");
    }

    @Test
    void deleteProduct_WhenNotFound_ShouldDoNothing() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        productService.deleteProduct(1L);

        verify(productRepository, never()).deleteById(any());
        verify(elasticsearchService, never()).deleteProduct(any());
    }

    @Test
    void sendProductUpdate_ShouldSendToKafka() {
        ProductDTO dto = ProductDTO.builder()
                .name("Test Product")
                .description("Desc")
                .category("Electronics")
                .price(BigDecimal.valueOf(99.99))
                .stock(10)
                .build();

        productService.sendProductUpdate(dto);

        verify(kafkaTemplate).send("product-topic", dto);
    }

    @Test
    void reindexAllProducts_ShouldIndexAllDocs() {
        List<Product> all = List.of(savedProduct);
        when(productRepository.findAll()).thenReturn(all);

        try (MockedStatic<ProductDocument> mocked = mockStatic(ProductDocument.class)) {
            mocked.when(() -> ProductDocument.fromProduct(savedProduct)).thenReturn(productDoc);

            productService.reindexAllProducts();

            verify(productRepository).findAll();
            verify(elasticsearchOperations).save(anyList());
        }
    }
}