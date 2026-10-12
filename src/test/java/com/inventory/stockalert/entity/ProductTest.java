package com.inventory.stockalert.entity;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.inventory.stockalert.enums.StockStatus;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    private Product createProduct(int quantity, int minimumStock) {
        return new Product(
                "Wireless Mouse",
                "MOUSE-001",
                "Wireless computer mouse",
                "Computer Accessories",
                new BigDecimal("249.99"),
                quantity,
                minimumStock
        );
    }

    @Test
    void stockAboveMinimumIsInStock() {
        Product product = createProduct(11, 10);

        assertEquals(StockStatus.IN_STOCK, product.getStockStatus());
    }

    @Test
    void stockExactlyAtMinimumIsLowStock() {
        Product product = createProduct(10, 10);

        assertEquals(StockStatus.LOW_STOCK, product.getStockStatus());
    }

    @Test
    void positiveStockBelowMinimumIsLowStock() {
        Product product = createProduct(5, 10);

        assertEquals(StockStatus.LOW_STOCK, product.getStockStatus());
    }

    @Test
    void zeroStockIsOutOfStockEvenWhenMinimumIsZero() {
        Product product = createProduct(0, 0);

        assertEquals(StockStatus.OUT_OF_STOCK, product.getStockStatus());
    }

    @Test
    void addingStockCanChangeLowStockToInStock() {
        Product product = createProduct(5, 10);

        product.addStock(10);

        assertEquals(15, product.getQuantity());
        assertEquals(StockStatus.IN_STOCK, product.getStockStatus());
    }

    @Test
    void removingStockCanChangeInStockToLowStock() {
        Product product = createProduct(20, 10);

        product.removeStock(15);

        assertEquals(5, product.getQuantity());
        assertEquals(StockStatus.LOW_STOCK, product.getStockStatus());
    }

    @Test
    void removingAllAvailableStockIsAllowed() {
        Product product = createProduct(5, 10);

        product.removeStock(5);

        assertEquals(0, product.getQuantity());
        assertEquals(StockStatus.OUT_OF_STOCK, product.getStockStatus());
    }

    @Test
    void insufficientStockIsRejectedWithoutChangingQuantity() {
        Product product = createProduct(5, 10);

        assertThrows(
                IllegalStateException.class,
                () -> product.removeStock(6)
        );

        assertEquals(5, product.getQuantity());
    }

    @Test
    void zeroAndNegativeAdditionsAreRejected() {
        Product product = createProduct(5, 10);

        assertThrows(
                IllegalArgumentException.class,
                () -> product.addStock(0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> product.addStock(-1)
        );

        assertEquals(5, product.getQuantity());
    }

    @Test
    void zeroAndNegativeRemovalsAreRejected() {
        Product product = createProduct(5, 10);

        assertThrows(
                IllegalArgumentException.class,
                () -> product.removeStock(0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> product.removeStock(-1)
        );

        assertEquals(5, product.getQuantity());
    }

    @Test
    void stockCanReachIntegerMaximumButCannotOverflow() {
        Product product = createProduct(Integer.MAX_VALUE - 1, 10);

        product.addStock(1);

        assertEquals(Integer.MAX_VALUE, product.getQuantity());

        assertThrows(
                IllegalArgumentException.class,
                () -> product.addStock(1)
        );

        assertEquals(Integer.MAX_VALUE, product.getQuantity());
    }

    @Test
    void inactiveProductRejectsBothStockOperations() {
        Product product = createProduct(5, 10);
        product.deactivate();

        assertThrows(
                IllegalStateException.class,
                () -> product.addStock(1)
        );

        assertThrows(
                IllegalStateException.class,
                () -> product.removeStock(1)
        );

        assertFalse(product.isActive());
        assertEquals(5, product.getQuantity());
    }

    @Test
    void updatingDetailsPreservesSkuAndQuantity() {
        Product product = createProduct(5, 10);

        product.updateDetails(
                "Updated Mouse",
                "Updated description",
                "Electronics",
                new BigDecimal("279.99"),
                3
        );

        assertAll(
                () -> assertEquals("Updated Mouse", product.getName()),
                () -> assertEquals(
                        "Updated description",
                        product.getDescription()
                ),
                () -> assertEquals("Electronics", product.getCategory()),
                () -> assertEquals(
                        new BigDecimal("279.99"),
                        product.getUnitPrice()
                ),
                () -> assertEquals(3, product.getMinimumStock()),
                () -> assertEquals("MOUSE-001", product.getSku()),
                () -> assertEquals(5, product.getQuantity()),
                () -> assertEquals(
                        StockStatus.IN_STOCK,
                        product.getStockStatus()
                )
        );
    }

    @Test
    void invalidUpdateLeavesExistingDetailsUnchanged() {
        Product product = createProduct(5, 10);

        assertThrows(
                IllegalArgumentException.class,
                () -> product.updateDetails(
                        "Changed Name",
                        "Changed description",
                        "Changed category",
                        new BigDecimal("100.00"),
                        -1
                )
        );

        assertAll(
                () -> assertEquals("Wireless Mouse", product.getName()),
                () -> assertEquals(
                        "Wireless computer mouse",
                        product.getDescription()
                ),
                () -> assertEquals(
                        "Computer Accessories",
                        product.getCategory()
                ),
                () -> assertEquals(
                        new BigDecimal("249.99"),
                        product.getUnitPrice()
                ),
                () -> assertEquals(10, product.getMinimumStock())
        );
    }

    @Test
    void negativeInitialQuantityIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> createProduct(-1, 10)
        );
    }
}