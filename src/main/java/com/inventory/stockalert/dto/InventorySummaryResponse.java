package com.inventory.stockalert.dto;

import java.math.BigDecimal;

public record InventorySummaryResponse(
        long totalActiveProducts,
        long totalUnitsInStock,
        long lowStockProducts,
        long outOfStockProducts,
        BigDecimal totalInventoryValue
) {
}