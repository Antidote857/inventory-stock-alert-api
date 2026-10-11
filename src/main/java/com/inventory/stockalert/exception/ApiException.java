package com.inventory.stockalert.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public static ApiException productNotFound(Long productId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "PRODUCT_NOT_FOUND",
                "Product not found with ID: " + productId
        );
    }

    public static ApiException duplicateSku(String sku) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "DUPLICATE_SKU",
                "A product already exists with SKU: " + sku
        );
    }

    public static ApiException insufficientStock(
            int available,
            int requested) {

        return new ApiException(
                HttpStatus.CONFLICT,
                "INSUFFICIENT_STOCK",
                "Insufficient stock. Available: " + available
                        + ", requested: " + requested + "."
        );
    }

    public static ApiException inactiveProduct(Long productId) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "PRODUCT_INACTIVE",
                "Product " + productId
                        + " is inactive and cannot receive stock movements."
        );
    }
}