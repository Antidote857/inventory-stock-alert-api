package com.inventory.stockalert.integration;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.inventory.stockalert.dto.CreateProductRequest;
import com.inventory.stockalert.dto.InventoryMovementResponse;
import com.inventory.stockalert.dto.ProductResponse;
import com.inventory.stockalert.dto.StockMovementRequest;
import com.inventory.stockalert.enums.MovementType;
import com.inventory.stockalert.exception.ApiException;
import com.inventory.stockalert.repository.InventoryMovementRepository;
import com.inventory.stockalert.repository.ProductRepository;
import com.inventory.stockalert.service.InventoryService;
import com.inventory.stockalert.service.ProductService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class InventoryIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryMovementRepository movementRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private DataSource dataSource;

    private String testSku;

    @BeforeEach
    void prepareTest() throws Exception {
        // Check the actual connection before creating any test records.
        try (Connection connection = dataSource.getConnection()) {
            assertEquals(
                    "inventory_test_db",
                    connection.getCatalog(),
                    "Integration tests must use inventory_test_db."
            );
        }

        testSku = "TEST-" + UUID.randomUUID();
    }

    @AfterEach
    void cleanUpOwnRecords() {
        if (testSku == null) {
            return;
        }

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            productRepository.findBySku(testSku).ifPresent(product -> {
                var movements = movementRepository
                        .findByProduct_IdOrderByCreatedAtDescIdDesc(
                                product.getId()
                        );

                // Delete only this test's records, children first.
                movementRepository.deleteAll(movements);
                movementRepository.flush();

                productRepository.delete(product);
                productRepository.flush();
            });
        });
    }

    private CreateProductRequest productRequest(int quantity) {
        return new CreateProductRequest(
                "Integration Test Mouse",
                testSku,
                "Product created by an integration test",
                "Test Products",
                new BigDecimal("249.99"),
                quantity,
                10
        );
    }

    private ProductResponse createProduct(int quantity) {
        return productService.createProduct(productRequest(quantity));
    }

    @Test
    void registrationPersistsProductAndInitialMovement() {
        ProductResponse created = createProduct(35);

        ProductResponse saved =
                productService.getProductById(created.id());

        List<InventoryMovementResponse> history =
                inventoryService.getMovements(created.id());

        assertAll(
                () -> assertEquals(testSku, saved.sku()),
                () -> assertEquals(35, saved.quantity()),
                () -> assertNotNull(saved.createdAt()),
                () -> assertTrue(saved.active()),
                () -> assertEquals(1, history.size())
        );

        InventoryMovementResponse movement = history.get(0);

        assertAll(
                () -> assertEquals(
                        MovementType.INITIAL_STOCK,
                        movement.movementType()
                ),
                () -> assertEquals(created.id(), movement.productId()),
                () -> assertEquals(35, movement.quantity()),
                () -> assertEquals(0, movement.previousStock()),
                () -> assertEquals(35, movement.newStock())
        );
    }

    @Test
    void zeroInitialStockCreatesNoMovement() {
        ProductResponse created = createProduct(0);

        assertEquals(
                0,
                productService.getProductById(created.id()).quantity()
        );

        assertTrue(
                inventoryService.getMovements(created.id()).isEmpty()
        );
    }

    @Test
    void stockAdditionPersistsQuantityAndMovement() {
        ProductResponse created = createProduct(35);

        InventoryMovementResponse added = inventoryService.addStock(
                created.id(),
                new StockMovementRequest(10, "Supplier delivery")
        );

        ProductResponse saved =
                productService.getProductById(created.id());

        List<InventoryMovementResponse> history =
                inventoryService.getMovements(created.id());

        assertAll(
                () -> assertEquals(45, saved.quantity()),
                () -> assertEquals(2, history.size()),
                () -> assertEquals(added.id(), history.get(0).id()),
                () -> assertEquals(
                        MovementType.STOCK_IN,
                        history.get(0).movementType()
                ),
                () -> assertEquals(35, history.get(0).previousStock()),
                () -> assertEquals(45, history.get(0).newStock())
        );
    }

    @Test
    void stockRemovalPersistsQuantityAndMovement() {
        ProductResponse created = createProduct(35);

        InventoryMovementResponse removed = inventoryService.removeStock(
                created.id(),
                new StockMovementRequest(30, "Customer order")
        );

        ProductResponse saved =
                productService.getProductById(created.id());

        List<InventoryMovementResponse> history =
                inventoryService.getMovements(created.id());

        assertAll(
                () -> assertEquals(5, saved.quantity()),
                () -> assertEquals(2, history.size()),
                () -> assertEquals(removed.id(), history.get(0).id()),
                () -> assertEquals(
                        MovementType.STOCK_OUT,
                        history.get(0).movementType()
                ),
                () -> assertEquals(35, history.get(0).previousStock()),
                () -> assertEquals(5, history.get(0).newStock())
        );
    }

    @Test
    void insufficientStockChangesNeitherProductNorHistory() {
        ProductResponse created = createProduct(5);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> inventoryService.removeStock(
                        created.id(),
                        new StockMovementRequest(100, "Excessive removal")
                )
        );

        assertAll(
                () -> assertEquals(
                        "INSUFFICIENT_STOCK",
                        exception.getCode()
                ),
                () -> assertEquals(
                        5,
                        productService.getProductById(created.id()).quantity()
                ),
                () -> assertEquals(
                        1,
                        inventoryService.getMovements(created.id()).size()
                )
        );
    }

    @Test
    void movementFailureRollsBackAlreadyFlushedStockUpdate() {
        ProductResponse created = createProduct(35);

        ProductResponse before =
                productService.getProductById(created.id());

        /*
         * Calling the service directly bypasses HTTP DTO validation.
         * InventoryService flushes the quantity update first.
         * The movement constructor then rejects this oversized reason.
         * The surrounding service transaction must roll back.
         */
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.addStock(
                        created.id(),
                        new StockMovementRequest(10, "X".repeat(501))
                )
        );

        assertEquals(
                "Reason cannot exceed 500 characters.",
                exception.getMessage()
        );

        ProductResponse after =
                productService.getProductById(created.id());

        List<InventoryMovementResponse> history =
                inventoryService.getMovements(created.id());

        assertAll(
                () -> assertEquals(35, after.quantity()),
                () -> assertEquals(before.updatedAt(), after.updatedAt()),
                () -> assertEquals(1, history.size()),
                () -> assertEquals(
                        MovementType.INITIAL_STOCK,
                        history.get(0).movementType()
                )
        );
    }

    @Test
    void duplicateSkuDoesNotCreateAnotherProductOrMovement() {
        ProductResponse original = createProduct(35);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> productService.createProduct(productRequest(10))
        );

        ProductResponse saved = productService.getProductBySku(testSku);

        assertAll(
                () -> assertEquals("DUPLICATE_SKU", exception.getCode()),
                () -> assertEquals(original.id(), saved.id()),
                () -> assertEquals(35, saved.quantity()),
                () -> assertEquals(
                        1,
                        inventoryService.getMovements(original.id()).size()
                )
        );
    }
}
