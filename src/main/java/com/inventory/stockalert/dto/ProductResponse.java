package com.inventory.stockalert.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.inventory.stockalert.entity.Product;
import com.inventory.stockalert.enums.StockStatus;

public record ProductResponse(
        Long id,
        String name,
        String sku,
        String description,
        String category,
        BigDecimal unitPrice,
        int quantity,
        int minimumStock,
        boolean active,
        StockStatus stockStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getDescription(),
                product.getCategory(),
                product.getUnitPrice(),
                product.getQuantity(),
                product.getMinimumStock(),
                product.isActive(),
                product.getStockStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}