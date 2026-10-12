package com.inventory.stockalert.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.inventory.stockalert.dto.InventoryMovementResponse;
import com.inventory.stockalert.dto.StockMovementRequest;
import com.inventory.stockalert.service.InventoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/{productId}/add")
    public InventoryMovementResponse addStock(
            @PathVariable("productId") Long productId,
            @Valid @RequestBody StockMovementRequest request) {

        return inventoryService.addStock(productId, request);
    }

    @PostMapping("/{productId}/remove")
    public InventoryMovementResponse removeStock(
            @PathVariable("productId") Long productId,
            @Valid @RequestBody StockMovementRequest request) {

        return inventoryService.removeStock(productId, request);
    }

    @GetMapping("/{productId}/movements")
    public List<InventoryMovementResponse> getMovements(
            @PathVariable("productId") Long productId) {

        return inventoryService.getMovements(productId);
    }
}