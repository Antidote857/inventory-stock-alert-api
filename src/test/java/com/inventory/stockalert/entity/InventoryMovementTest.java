package com.inventory.stockalert.entity;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.inventory.stockalert.enums.MovementType;

import static org.junit.jupiter.api.Assertions.*;

class InventoryMovementTest {

    private Product createProduct(int quantity) {
        return new Product(
                "Wireless Mouse",
                "MOUSE-001",
                "Wireless computer mouse",
                "Computer Accessories",
                new BigDecimal("249.99"),
                quantity,
                10
        );
    }

    @Test
    void initialStockRecordsOpeningQuantity() {
        Product product = createProduct(35);

        InventoryMovement movement = new InventoryMovement(
                product,
                MovementType.INITIAL_STOCK,
                35,
                0,
                35,
                "Opening stock"
        );

        assertAll(
                () -> assertSame(product, movement.getProduct()),
                () -> assertEquals(
                        MovementType.INITIAL_STOCK,
                        movement.getMovementType()
                ),
                () -> assertEquals(35, movement.getQuantity()),
                () -> assertEquals(0, movement.getPreviousStock()),
                () -> assertEquals(35, movement.getNewStock()),
                () -> assertEquals("Opening stock", movement.getReason())
        );
    }

    @Test
    void stockInRecordsAnIncrease() {
        Product product = createProduct(35);
        product.addStock(10);

        InventoryMovement movement = new InventoryMovement(
                product,
                MovementType.STOCK_IN,
                10,
                35,
                45,
                "Supplier delivery"
        );

        assertAll(
                () -> assertEquals(
                        MovementType.STOCK_IN,
                        movement.getMovementType()
                ),
                () -> assertEquals(10, movement.getQuantity()),
                () -> assertEquals(35, movement.getPreviousStock()),
                () -> assertEquals(45, movement.getNewStock())
        );
    }

    @Test
    void stockOutCanReduceQuantityToZero() {
        Product product = createProduct(5);
        product.removeStock(5);

        InventoryMovement movement = new InventoryMovement(
                product,
                MovementType.STOCK_OUT,
                5,
                5,
                0,
                "Final units sold"
        );

        assertAll(
                () -> assertEquals(
                        MovementType.STOCK_OUT,
                        movement.getMovementType()
                ),
                () -> assertEquals(5, movement.getQuantity()),
                () -> assertEquals(5, movement.getPreviousStock()),
                () -> assertEquals(0, movement.getNewStock())
        );
    }

    @Test
    void initialStockMustStartFromZero() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InventoryMovement(
                        createProduct(35),
                        MovementType.INITIAL_STOCK,
                        35,
                        1,
                        35,
                        "Opening stock"
                )
        );
    }

    @Test
    void initialStockMustMatchMovementQuantity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InventoryMovement(
                        createProduct(35),
                        MovementType.INITIAL_STOCK,
                        34,
                        0,
                        35,
                        "Opening stock"
                )
        );
    }

    @Test
    void stockInRejectsIncorrectArithmetic() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InventoryMovement(
                        createProduct(44),
                        MovementType.STOCK_IN,
                        10,
                        35,
                        44,
                        "Supplier delivery"
                )
        );
    }

    @Test
    void stockOutRejectsIncorrectArithmetic() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InventoryMovement(
                        createProduct(6),
                        MovementType.STOCK_OUT,
                        40,
                        45,
                        6,
                        "Customer order"
                )
        );
    }

    @Test
    void movementMustMatchCurrentProductQuantity() {
        // The arithmetic is valid, but the product still holds 35 units.
        assertThrows(
                IllegalArgumentException.class,
                () -> new InventoryMovement(
                        createProduct(35),
                        MovementType.STOCK_IN,
                        10,
                        35,
                        45,
                        "Supplier delivery"
                )
        );
    }

    @Test
    void zeroAndNegativeMovementQuantitiesAreRejected() {
        for (int amount : new int[] {0, -1}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new InventoryMovement(
                            createProduct(5),
                            MovementType.STOCK_IN,
                            amount,
                            5,
                            5,
                            "Invalid quantity"
                    )
            );
        }
    }

    @Test
    void negativePreviousStockIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InventoryMovement(
                        createProduct(5),
                        MovementType.STOCK_IN,
                        10,
                        -5,
                        5,
                        "Invalid previous stock"
                )
        );
    }

    @Test
    void negativeNewStockIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InventoryMovement(
                        createProduct(0),
                        MovementType.STOCK_OUT,
                        6,
                        5,
                        -1,
                        "Invalid new stock"
                )
        );
    }

    @Test
    void missingProductIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InventoryMovement(
                        null,
                        MovementType.INITIAL_STOCK,
                        5,
                        0,
                        5,
                        "Opening stock"
                )
        );
    }

    @Test
    void missingMovementTypeIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InventoryMovement(
                        createProduct(5),
                        null,
                        5,
                        0,
                        5,
                        "Opening stock"
                )
        );
    }

    @Test
    void inactiveProductCannotReceiveMovementRecords() {
        Product product = createProduct(5);
        product.deactivate();

        assertThrows(
                IllegalStateException.class,
                () -> new InventoryMovement(
                        product,
                        MovementType.INITIAL_STOCK,
                        5,
                        0,
                        5,
                        "Opening stock"
                )
        );
    }

    @Test
    void nullEmptyAndWhitespaceReasonsAreRejected() {
        for (String reason : new String[] {null, "", "   "}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new InventoryMovement(
                            createProduct(5),
                            MovementType.INITIAL_STOCK,
                            5,
                            0,
                            5,
                            reason
                    )
            );
        }
    }

    @Test
    void reasonIsTrimmedAndAllowsExactly500Characters() {
        String reason = "A".repeat(500);

        InventoryMovement movement = new InventoryMovement(
                createProduct(5),
                MovementType.INITIAL_STOCK,
                5,
                0,
                5,
                "  " + reason + "  "
        );

        assertEquals(reason, movement.getReason());
    }

    @Test
    void reasonLongerThan500CharactersIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InventoryMovement(
                        createProduct(5),
                        MovementType.INITIAL_STOCK,
                        5,
                        0,
                        5,
                        "A".repeat(501)
                )
        );
    }
}