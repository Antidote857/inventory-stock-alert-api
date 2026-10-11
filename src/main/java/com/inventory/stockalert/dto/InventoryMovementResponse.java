package com.inventory.stockalert.dto;

import java.time.OffsetDateTime;

import com.inventory.stockalert.entity.InventoryMovement;
import com.inventory.stockalert.enums.MovementType;

public record InventoryMovementResponse(
        Long id,
        Long productId,
        MovementType movementType,
        int quantity,
        int previousStock,
        int newStock,
        String reason,
        OffsetDateTime createdAt
) {

    public static InventoryMovementResponse from(
            InventoryMovement movement) {

        return new InventoryMovementResponse(
                movement.getId(),
                movement.getProduct().getId(),
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getPreviousStock(),
                movement.getNewStock(),
                movement.getReason(),
                movement.getCreatedAt()
        );
    }
}