package com.inventory.stockalert.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.inventory.stockalert.dto.ApiError;
import com.inventory.stockalert.dto.InventoryMovementResponse;
import com.inventory.stockalert.dto.StockMovementRequest;
import com.inventory.stockalert.service.InventoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/inventory")
@Tag(
        name = "Inventory Movements",
        description = "Add stock, remove stock, and retrieve movement history."
)
@ApiResponse(
        responseCode = "400",
        description = "Invalid product ID, request body, or field value.",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
        )
)
@ApiResponse(
        responseCode = "404",
        description = "PRODUCT_NOT_FOUND.",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
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
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/{productId}/add")
    @Operation(
            summary = "Add stock to a product",
            description = """
                    Adds a positive quantity to an active product.
                    A nonblank reason is required.
                    Updates the product and records a STOCK_IN movement
                    within the same transaction.

                    Returns 400 with STOCK_LIMIT_EXCEEDED if the resulting
                    quantity would exceed 2147483647.
                    """,
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            implementation = StockMovementRequest.class
                                    ),
                                    examples = @ExampleObject(
                                            name = "Supplier delivery",
                                            value = """
                                                    {
                                                      "quantity": 10,
                                                      "reason": "Received supplier delivery"
                                                    }
                                                    """
                                    )
                            )
                    )
    )
    @ApiResponse(
            responseCode = "200",
            description = "Stock added; returns the saved movement record.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            implementation = InventoryMovementResponse.class
                    )
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = """
                    PRODUCT_INACTIVE, CONCURRENT_UPDATE,
                    or another database data conflict.
                    """,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiError.class)
            )
    )
    public InventoryMovementResponse addStock(
            @Parameter(description = "Positive product ID.", example = "2")
            @PathVariable("productId") Long productId,
            @Valid @RequestBody StockMovementRequest request) {

        return inventoryService.addStock(productId, request);
    }

    @PostMapping("/{productId}/remove")
    @Operation(
            summary = "Remove stock from a product",
            description = """
                    Removes a positive quantity from an active product.
                    A nonblank reason is required.
                    The requested quantity must not exceed available stock.

                    Updates the product and records a STOCK_OUT movement
                    within the same transaction. Rejected operations
                    leave stock and movement history unchanged.
                    """,
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            implementation = StockMovementRequest.class
                                    ),
                                    examples = @ExampleObject(
                                            name = "Customer order",
                                            value = """
                                                    {
                                                      "quantity": 3,
                                                      "reason": "Customer order fulfilled"
                                                    }
                                                    """
                                    )
                            )
                    )
    )
    @ApiResponse(
            responseCode = "200",
            description = "Stock removed; returns the saved movement record.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            implementation = InventoryMovementResponse.class
                    )
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = """
                    INSUFFICIENT_STOCK, PRODUCT_INACTIVE,
                    CONCURRENT_UPDATE, or another database data conflict.
                    """,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiError.class)
            )
    )
    public InventoryMovementResponse removeStock(
            @Parameter(description = "Positive product ID.", example = "2")
            @PathVariable("productId") Long productId,
            @Valid @RequestBody StockMovementRequest request) {

        return inventoryService.removeStock(productId, request);
    }

    @GetMapping("/{productId}/movements")
    @Operation(
            summary = "Retrieve product movement history",
            description = """
                    Returns movements newest first, ordered by creation
                    timestamp and then movement ID descending.

                    History remains available for inactive products.
                    An existing product with no movements returns an empty array.
                    A nonexistent product returns 404.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Product movement history.",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(
                            schema = @Schema(
                                    implementation = InventoryMovementResponse.class
                            )
                    )
            )
    )
    public List<InventoryMovementResponse> getMovements(
            @Parameter(description = "Positive product ID.", example = "1")
            @PathVariable("productId") Long productId) {

        return inventoryService.getMovements(productId);
    }
}