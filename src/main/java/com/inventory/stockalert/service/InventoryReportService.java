package com.inventory.stockalert.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.stockalert.dto.InventorySummaryResponse;
import com.inventory.stockalert.dto.ProductResponse;
import com.inventory.stockalert.entity.Product;
import com.inventory.stockalert.enums.StockStatus;
import com.inventory.stockalert.repository.ProductRepository;

@Service
@Transactional(readOnly = true)
public class InventoryReportService {

    private final ProductRepository productRepository;

    public InventoryReportService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> getRestockAlerts() {
        return productRepository.findProductsNeedingRestock()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    public List<ProductResponse> getLowStockAlerts() {
        return productRepository.findLowStockProducts()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    public List<ProductResponse> getOutOfStockAlerts() {
        return productRepository.findOutOfStockProducts()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    public InventorySummaryResponse getSummary() {
        // Read active products once so every total uses the same dataset.
        List<Product> products =
                productRepository.findByActiveTrueOrderByIdAsc();

        long totalUnits = 0;
        long lowStockCount = 0;
        long outOfStockCount = 0;
        BigDecimal totalValue = new BigDecimal("0.00");

        for (Product product : products) {
            totalUnits += product.getQuantity();

            StockStatus status = product.getStockStatus();

            if (status == StockStatus.OUT_OF_STOCK) {
                outOfStockCount++;
            } else if (status == StockStatus.LOW_STOCK) {
                lowStockCount++;
            }

            BigDecimal productValue = product.getUnitPrice()
                    .multiply(BigDecimal.valueOf(product.getQuantity()));

            totalValue = totalValue.add(productValue);
        }

        return new InventorySummaryResponse(
                products.size(),
                totalUnits,
                lowStockCount,
                outOfStockCount,
                totalValue
        );
    }
}