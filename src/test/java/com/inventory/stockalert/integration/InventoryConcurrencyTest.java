package com.inventory.stockalert.integration;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.inventory.stockalert.dto.CreateProductRequest;
import com.inventory.stockalert.dto.InventoryMovementResponse;
import com.inventory.stockalert.dto.ProductResponse;
import com.inventory.stockalert.dto.StockMovementRequest;
import com.inventory.stockalert.enums.MovementType;
import com.inventory.stockalert.repository.InventoryMovementRepository;
import com.inventory.stockalert.repository.ProductRepository;
import com.inventory.stockalert.service.InventoryService;
import com.inventory.stockalert.service.ProductService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class InventoryConcurrencyTest {

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
        try (Connection connection = dataSource.getConnection()) {
            assertEquals(
                    "inventory_test_db",
                    connection.getCatalog(),
                    "Concurrency tests must use inventory_test_db."
            );
        }

        testSku = "CONCURRENT-" + UUID.randomUUID();
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

                movementRepository.deleteAll(movements);
                movementRepository.flush();

                productRepository.delete(product);
                productRepository.flush();
            });
        });
    }

    @Test
    void competingRemovalsCommitExactlyOneStockChange() throws Exception {
        ProductResponse created = productService.createProduct(
                new CreateProductRequest(
                        "Concurrency Test Mouse",
                        testSku,
                        "Temporary concurrency test product",
                        "Test Products",
                        new BigDecimal("100.00"),
                        5,
                        2
                )
        );

        CountDownLatch bothLoaded = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(
                    () -> attemptRemoval(created.id(), bothLoaded)
            );

            Future<Boolean> second = executor.submit(
                    () -> attemptRemoval(created.id(), bothLoaded)
            );

            boolean firstSucceeded = first.get(30, TimeUnit.SECONDS);
            boolean secondSucceeded = second.get(30, TimeUnit.SECONDS);

            assertNotEquals(
                    firstSucceeded,
                    secondSucceeded,
                    "Exactly one transaction must succeed."
            );

            ProductResponse saved =
                    productService.getProductById(created.id());

            List<InventoryMovementResponse> history =
                    inventoryService.getMovements(created.id());

            assertEquals(1, saved.quantity());
            assertEquals(
                    2,
                    history.size(),
                    "Expected initial stock and one successful removal."
            );

            List<InventoryMovementResponse> removals = history.stream()
                    .filter(movement ->
                            movement.movementType() == MovementType.STOCK_OUT)
                    .toList();

            assertEquals(1, removals.size());

            InventoryMovementResponse removal = removals.get(0);

            assertAll(
                    () -> assertEquals(4, removal.quantity()),
                    () -> assertEquals(5, removal.previousStock()),
                    () -> assertEquals(1, removal.newStock())
            );
        } finally {
            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(30, TimeUnit.SECONDS),
                    "Concurrent test workers did not terminate."
            );
        }
    }

    private boolean attemptRemoval(
            Long productId,
            CountDownLatch bothLoaded) {

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.setTimeout(20);

        try {
            transaction.executeWithoutResult(status -> {
                /*
                 * Each thread has its own transaction and persistence context.
                 * Load the product before either thread changes it.
                 */
                productRepository.findById(productId).orElseThrow();

                bothLoaded.countDown();
                awaitBothTransactions(bothLoaded);

                /*
                 * The real service joins this transaction and reuses its
                 * already-loaded product, forcing a stale-version conflict
                 * for whichever transaction updates second.
                 */
                inventoryService.removeStock(
                        productId,
                        new StockMovementRequest(
                                4,
                                "Concurrent customer order"
                        )
                );
            });

            return true;
        } catch (OptimisticLockingFailureException exception) {
            return false;
        }
    }

    private void awaitBothTransactions(CountDownLatch bothLoaded) {
        try {
            if (!bothLoaded.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Timed out waiting for both transactions to load."
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Concurrent test was interrupted.",
                    exception
            );
        }
    }
}
