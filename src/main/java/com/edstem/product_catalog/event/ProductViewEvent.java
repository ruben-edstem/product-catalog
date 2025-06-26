package com.edstem.product_catalog.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductViewEvent {
    private Long productId;
    private String userId;
    private LocalDateTime viewedAt;
    private String source;
}