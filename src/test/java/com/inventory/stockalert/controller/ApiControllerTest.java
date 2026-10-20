package com.inventory.stockalert.controller;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.inventory.stockalert.dto.CreateProductRequest;
import com.inventory.stockalert.dto.ProductResponse;
import com.inventory.stockalert.dto.StockMovementRequest;
import com.inventory.stockalert.enums.StockStatus;
import com.inventory.stockalert.exception.ApiException;
import com.inventory.stockalert.exception.GlobalExceptionHandler;
import com.inventory.stockalert.service.InventoryService;
import com.inventory.stockalert.service.ProductService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        ProductController.class,
        InventoryController.class
})
@Import(GlobalExceptionHandler.class)
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private InventoryService inventoryService;

    private static final String VALID_PRODUCT_JSON = """
            {
                "name": "Wireless Mouse",
                "sku": "MOUSE-001",
                "description": "Wireless computer mouse",
                "category": "Computer Accessories",
                "unitPrice": 249.99,
                "quantity": 35,
                "minimumStock": 10
            }
            """;

    private ProductResponse sampleProduct() {
        OffsetDateTime timestamp =
                OffsetDateTime.parse("2026-10-20T12:00:00Z");

        return new ProductResponse(
                1L,
                "Wireless Mouse",
                "MOUSE-001",
                "Wireless computer mouse",
                "Computer Accessories",
                new BigDecimal("249.99"),
                35,
                10,
                true,
                StockStatus.IN_STOCK,
                timestamp,
                timestamp
        );
    }

    @Test
    void creationReturns201LocationAndProductJson() throws Exception {
        when(productService.createProduct(any(CreateProductRequest.class)))
                .thenReturn(sampleProduct());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PRODUCT_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/products/1"))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("MOUSE-001"))
                .andExpect(jsonPath("$.quantity").value(35))
                .andExpect(jsonPath("$.stockStatus").value("IN_STOCK"))
                .andExpect(jsonPath("$.active").value(true));

        verify(productService).createProduct(
                new CreateProductRequest(
                        "Wireless Mouse",
                        "MOUSE-001",
                        "Wireless computer mouse",
                        "Computer Accessories",
                        new BigDecimal("249.99"),
                        35,
                        10
                )
        );
    }

    @Test
    void invalidProductReturns400WithFieldErrors() throws Exception {
        String invalidJson = """
                {
                    "name": "",
                    "sku": "INVALID-001",
                    "unitPrice": -10,
                    "quantity": -5,
                    "minimumStock": 0
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/products"))
                .andExpect(jsonPath("$.fieldErrors.name").isArray())
                .andExpect(jsonPath("$.fieldErrors.unitPrice").isArray())
                .andExpect(jsonPath("$.fieldErrors.quantity").isArray());

        verifyNoInteractions(productService);
    }

    @Test
    void missingProductReturns404() throws Exception {
        when(productService.getProductById(999L))
                .thenThrow(ApiException.productNotFound(999L));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(
                        "Product not found with ID: 999"
                ))
                .andExpect(jsonPath("$.path").value("/api/products/999"));
    }

    @Test
    void duplicateSkuReturns409() throws Exception {
        when(productService.createProduct(any(CreateProductRequest.class)))
                .thenThrow(ApiException.duplicateSku("MOUSE-001"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PRODUCT_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("DUPLICATE_SKU"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "INVALID_REQUEST_BODY"
                ));

        verifyNoInteractions(productService);
    }

    @Test
    void invalidStockRequestReturns400BeforeCallingService()
            throws Exception {

        String invalidJson = """
                {
                    "quantity": 0,
                    "reason": ""
                }
                """;

        mockMvc.perform(post("/api/inventory/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.quantity").isArray())
                .andExpect(jsonPath("$.fieldErrors.reason").isArray());

        verifyNoInteractions(inventoryService);
    }

    @Test
    void insufficientStockReturns409() throws Exception {
        when(inventoryService.removeStock(
                eq(1L),
                any(StockMovementRequest.class)
        )).thenThrow(ApiException.insufficientStock(5, 100));

        mockMvc.perform(post("/api/inventory/1/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "quantity": 100,
                                    "reason": "Customer order"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value(
                        "INSUFFICIENT_STOCK"
                ))
                .andExpect(jsonPath("$.message").value(
                        "Insufficient stock. Available: 5, requested: 100."
                ));
    }

    @Test
    void optimisticLockConflictReturns409() throws Exception {
        when(inventoryService.removeStock(
                eq(1L),
                any(StockMovementRequest.class)
        )).thenThrow(
                new OptimisticLockingFailureException("Version conflict")
        );

        mockMvc.perform(post("/api/inventory/1/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "quantity": 4,
                                    "reason": "Concurrent customer order"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value(
                        "CONCURRENT_UPDATE"
                ))
                .andExpect(jsonPath("$.message").value(
                        "The product changed during this operation. "
                                + "Reload it and try again."
                ));
    }
}