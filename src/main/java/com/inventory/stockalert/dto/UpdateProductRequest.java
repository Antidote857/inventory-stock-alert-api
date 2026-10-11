package com.inventory.stockalert.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateProductRequest(

        @NotBlank(message = "Product name is required.")
        @Size(max = 150, message = "Product name cannot exceed 150 characters.")
        String name,

        String description,

        @Size(max = 100, message = "Category cannot exceed 100 characters.")
        String category,

        @NotNull(message = "Unit price is required.")
        @PositiveOrZero(message = "Unit price cannot be negative.")
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Unit price allows up to 17 whole-number digits and 2 decimal places."
        )
        BigDecimal unitPrice,

        @NotNull(message = "Minimum stock is required.")
        @PositiveOrZero(message = "Minimum stock cannot be negative.")
        Integer minimumStock
) {
}