package com.inventory.stockalert.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inventory.stockalert.dto.ProductResponse;
import com.inventory.stockalert.service.InventoryReportService;

@RestController
@RequestMapping("/api/alerts")
public class StockAlertController {

    private final InventoryReportService reportService;

    public StockAlertController(InventoryReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public List<ProductResponse> getRestockAlerts() {
        return reportService.getRestockAlerts();
    }

    @GetMapping("/low-stock")
    public List<ProductResponse> getLowStockAlerts() {
        return reportService.getLowStockAlerts();
    }

    @GetMapping("/out-of-stock")
    public List<ProductResponse> getOutOfStockAlerts() {
        return reportService.getOutOfStockAlerts();
    }
}