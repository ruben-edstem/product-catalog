package com.edstem.product_catalog.service;

import com.edstem.product_catalog.contract.ProductDTO;
import com.edstem.product_catalog.document.ProductDocument;
import com.edstem.product_catalog.model.Product;
import com.edstem.product_catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final GenericCacheService cacheService;
    private final KafkaTemplate<String, ProductDTO> kafkaTemplate;
    private final ElasticsearchService elasticsearchService;
    private final ElasticsearchOperations elasticsearchOperations;

    private static final String PRODUCT_CACHE_PREFIX = "product:";
    private static final String PRODUCT_LIST_CACHE = "products:all";
    private static final String PRODUCT_TOPIC = "product-topic";

    private static final Duration PRODUCT_CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration LIST_CACHE_TTL = Duration.ofMinutes(5);

    public Product createProduct(Product product) {
        log.info("Creating product in DB: {}", product);
        Product toSave = product.toBuilder().build();
        Product saved = productRepository.save(toSave);

        ProductDocument doc = ProductDocument.fromProduct(saved);
        elasticsearchOperations.save(doc);
        log.info("Indexed product {} into Elasticsearch", doc.getId());

        return saved;
    }

    @SneakyThrows
    public Product getProductById(Long id) {
        String cacheKey = PRODUCT_CACHE_PREFIX + id;

        Product cachedProduct = cacheService.getCachedObject(cacheKey, Product.class);
        if (cachedProduct != null) {
            log.info("Product found in cache for id: {}", id);
            return cachedProduct;
        }

        log.info("Fetching product from database for id: {}", id);
        Thread.sleep(1000);
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            cacheService.cacheObject(cacheKey, product, PRODUCT_CACHE_TTL);
            return product;
        }

        return null;
    }

    @SneakyThrows
    public List<Product> getAllProducts() {
        List<Product> cachedProducts = cacheService.getCachedObject(PRODUCT_LIST_CACHE, List.class);
        if (cachedProducts != null) {
            log.info("Products list found in cache");
            return cachedProducts;
        }

        log.info("Fetching all products from database");
        List<Product> products = productRepository.findAll();
        Thread.sleep(1000);

        if (!products.isEmpty()) {
            cacheService.cacheObject(PRODUCT_LIST_CACHE, products, LIST_CACHE_TTL);
            products.forEach(product -> {
                cacheService.cacheObject(PRODUCT_CACHE_PREFIX + product.getId(), product, PRODUCT_CACHE_TTL);
            });
        }

        return products;
    }

    public Product updateProduct(Long id, Product productDetails) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isPresent()) {
            Product existingProduct = productOpt.get();
            existingProduct.setName(productDetails.getName());
            existingProduct.setDescription(productDetails.getDescription());
            existingProduct.setCategory(productDetails.getCategory());
            existingProduct.setPrice(productDetails.getPrice());
            existingProduct.setStock(productDetails.getStock());

            log.info("Updating product in database: {}", id);
            Product updatedProduct = productRepository.save(existingProduct);

            cacheService.cacheObject(PRODUCT_CACHE_PREFIX + id, updatedProduct, PRODUCT_CACHE_TTL);
            cacheService.evictCache(PRODUCT_LIST_CACHE);

            elasticsearchService.updateProduct(updatedProduct);

            log.info("Product cache updated, Elasticsearch updated, and list cache invalidated");
            return updatedProduct;
        }
        return null;
    }

    public void deleteProduct(Long id) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isPresent()) {
            log.info("Deleting product from database: {}", id);
            productRepository.deleteById(id);

            cacheService.evictCache(PRODUCT_CACHE_PREFIX + id);
            cacheService.evictCache(PRODUCT_LIST_CACHE);

            elasticsearchService.deleteProduct(String.valueOf(id));

            log.info("Product deleted from database, cache, and Elasticsearch");
        }
    }

    @Async
    public void sendProductUpdate(ProductDTO product) {
        kafkaTemplate.send(PRODUCT_TOPIC, product);
    }

    public void reindexAllProducts() {
        List<Product> allProducts = productRepository.findAll();
        List<ProductDocument> docs = allProducts.stream()
                .map(ProductDocument::fromProduct)
                .toList();

        elasticsearchOperations.save(docs);
        log.info("Reindexed {} products into Elasticsearch", docs.size());
    }

    public static ProductDTO toDto(Product product) {
        return ProductDTO.builder()
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .price(product.getPrice())
                .stock(product.getStock())
                .build();
    }
}
