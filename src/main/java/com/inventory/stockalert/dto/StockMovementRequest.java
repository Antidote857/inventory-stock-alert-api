package com.inventory.stockalert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StockMovementRequest(

        @NotNull(message = "Movement quantity is required.")
        @Positive(message = "Movement quantity must be greater than zero.")
        Integer quantity,

        @NotBlank(message = "Reason is required.")
        @Size(max = 500, message = "Reason cannot exceed 500 characters.")
        String reason
) {
}