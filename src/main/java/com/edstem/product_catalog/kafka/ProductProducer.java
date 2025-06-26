package com.edstem.product_catalog.kafka;

import com.edstem.product_catalog.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductProducer {

    private final KafkaTemplate<String, Product> kafkaTemplate;

    @Async
    public void sendProduct(Product product) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        kafkaTemplate.send("product-topic", String.valueOf(product.getId() % 5), product);
        log.info("Sent product: {}", product);
    }

    public void sendProducts() {
        IntStream.range(0, 10).forEach(i -> {
            Product product = Product.builder()
                    .id((long) i)
                    .name("Product" + i)
                    .description("Description for product " + i)
                    .category(i % 2 == 0 ? "Electronics" : "Books")
                    .price(BigDecimal.valueOf(99.99 + i * 10))
                    .stock(100 - i * 5)
                    .build();
            sendProduct(product);
        });
    }

    public void flush() {
        kafkaTemplate.flush();
    }
}
