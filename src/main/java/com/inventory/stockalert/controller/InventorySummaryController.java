package com.inventory.stockalert.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inventory.stockalert.dto.InventorySummaryResponse;
import com.inventory.stockalert.service.InventoryReportService;

@RestController
@RequestMapping("/api/inventory")
public class InventorySummaryController {

    private final InventoryReportService reportService;

    public InventorySummaryController(
            InventoryReportService reportService) {

        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public InventorySummaryResponse getSummary() {
        return reportService.getSummary();
    }
}