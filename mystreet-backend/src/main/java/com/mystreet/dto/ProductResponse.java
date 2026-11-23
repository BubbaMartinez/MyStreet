package com.mystreet.dto;

import com.mystreet.domain.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String brand,
        String description,
        BigDecimal price,
        String imageUrl,
        List<String> sizes,
        int stockQty) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getBrand(), p.getDescription(),
                p.getPrice(), p.getImageUrl(),
                p.getSizes().stream().sorted().toList(),
                p.getStockQty());
    }
}
