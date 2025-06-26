package com.edstem.product_catalog.kafka;

import com.edstem.product_catalog.model.Product;
import com.edstem.product_catalog.service.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductElasticsearchConsumer {

    private final ElasticsearchService elasticsearchService;

    @KafkaListener(topics = "product-topic", groupId = "elasticsearch-consumer-group")
    public void consumeProductForIndexing(Product product) {
        try {
            log.info("Received product for Elasticsearch indexing: {}", product.getName());

            Product productToIndex = Product.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .description(product.getDescription())
                    .category(product.getCategory())
                    .price(product.getPrice())
                    .stock(product.getStock())
                    .build();

            elasticsearchService.indexProduct(productToIndex);

            log.info("Successfully indexed product in Elasticsearch: {}", product.getName());
        } catch (Exception e) {
            log.error("Error indexing product in Elasticsearch: {}", e.getMessage(), e);
        }
    }
}
