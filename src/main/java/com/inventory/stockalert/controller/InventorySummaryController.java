package com.inventory.stockalert.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inventory.stockalert.dto.ApiError;
import com.inventory.stockalert.dto.InventorySummaryResponse;
import com.inventory.stockalert.service.InventoryReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/inventory")
@Tag(
        name = "Inventory Summary",
        description = "View totals and inventory value for active products."
)
public class InventorySummaryController {

    private final InventoryReportService reportService;

    public InventorySummaryController(
            InventoryReportService reportService) {

        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @Operation(
            summary = "Retrieve the inventory summary",
            description = """
                    Calculates totals using active products only.

                    totalActiveProducts: number of active product records.
                    totalUnitsInStock: sum of their stock quantities.
                    lowStockProducts: positive stock at or below the minimum.
                    outOfStockProducts: zero-stock products.
                    totalInventoryValue: sum of unit price multiplied by quantity.

                    Low-stock and out-of-stock counts do not overlap.
                    If no products are active, all totals are zero.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Current inventory summary.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            implementation = InventorySummaryResponse.class
                    ),
                    examples = @ExampleObject(
                            name = "One active keyboard product",
                            value = """
                                    {
                                      "totalActiveProducts": 1,
                                      "totalUnitsInStock": 20,
                                      "lowStockProducts": 0,
                                      "outOfStockProducts": 0,
                                      "totalInventoryValue": 7999.80
                                    }
                                    """
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
    public InventorySummaryResponse getSummary() {
        return reportService.getSummary();
    }
}