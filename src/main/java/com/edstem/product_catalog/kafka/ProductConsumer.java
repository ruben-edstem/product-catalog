package com.edstem.product_catalog.kafka;

import com.edstem.product_catalog.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class ProductConsumer {

    @KafkaListener(topics = "product-topic", groupId = "product-consumer-group", concurrency = "3")
    public void listen(Product product, @Header(KafkaHeaders.RECEIVED_PARTITION) String partition,
                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                       @Header(KafkaHeaders.OFFSET) String offset,
                       @Header(KafkaHeaders.RECEIVED_TIMESTAMP) String timestamp) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 4000));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        log.info("Received product: {}", product);
        log.info("Thread: {}, Topic: {}, Partition: {}, Offset: {}, Timestamp: {}",
                Thread.currentThread().getName(), topic, partition, offset, timestamp);
    }

    @KafkaListener(topics = "product-views", groupId = "product-view-consumer-group")
    public void handleProductView(com.edstem.product_catalog.event.ProductViewEvent event) {
        log.info("Processing product view event: ProductId={}, UserId={}, Time={}",
                event.getProductId(),
                event.getUserId(),
                event.getViewedAt());
    }
}