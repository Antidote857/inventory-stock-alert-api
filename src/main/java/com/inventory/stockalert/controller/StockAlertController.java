package com.inventory.stockalert.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inventory.stockalert.dto.ApiError;
import com.inventory.stockalert.dto.ProductResponse;
import com.inventory.stockalert.service.InventoryReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/alerts")
@Tag(
        name = "Stock Alerts",
        description = "Identify active products that need restocking."
)
@ApiResponse(
        responseCode = "200",
        description = "Matching active products, or an empty array.",
        content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(
                        schema = @Schema(implementation = ProductResponse.class)
                )
        )
)
@ApiResponse(
        responseCode = "500",
        description = "Unexpected server error.",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
        )
)
public class StockAlertController {

    private final InventoryReportService reportService;

    public StockAlertController(InventoryReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    @Operation(
            summary = "Retrieve all restocking alerts",
            description = """
                    Returns active products whose quantity is at or below
                    their minimum stock threshold, including zero-stock products.
                    Results are ordered by quantity ascending, then ID ascending.
                    """
    )
    public List<ProductResponse> getRestockAlerts() {
        return reportService.getRestockAlerts();
    }

    @GetMapping("/low-stock")
    @Operation(
            summary = "Retrieve low-stock products",
            description = """
                    Returns active products with a positive quantity at or below
                    their minimum stock threshold. Zero-stock products are excluded.
                    Results are ordered by quantity ascending, then ID ascending.
                    """
    )
    public List<ProductResponse> getLowStockAlerts() {
        return reportService.getLowStockAlerts();
    }

    @GetMapping("/out-of-stock")
    @Operation(
            summary = "Retrieve out-of-stock products",
            description = """
                    Returns active products with a quantity of zero,
                    ordered by ID ascending.
                    """
    )
    public List<ProductResponse> getOutOfStockAlerts() {
        return reportService.getOutOfStockAlerts();
    }
}