package com.inventory.stockalert.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.inventory.stockalert.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Includes inactive products so their SKUs remain reserved.
    boolean existsBySku(String sku);

    // Allows historical products to be retrieved by SKU.
    Optional<Product> findBySku(String sku);

    // Default product listing: active products only.
    List<Product> findByActiveTrueOrderByIdAsc();

    // Case-insensitive partial name search.
    List<Product> findByActiveTrueAndNameContainingIgnoreCaseOrderByIdAsc(
            String name
    );

    // Case-insensitive exact category match.
    List<Product> findByActiveTrueAndCategoryIgnoreCaseOrderByIdAsc(
            String category
    );

    // All active products needing restocking, including zero stock.
    @Query("""
            SELECT p
            FROM Product p
            WHERE p.active = true
              AND p.quantity <= p.minimumStock
            ORDER BY p.quantity ASC, p.id ASC
            """)
    List<Product> findProductsNeedingRestock();

    // Low stock excludes products that are completely out of stock.
    @Query("""
            SELECT p
            FROM Product p
            WHERE p.active = true
              AND p.quantity > 0
              AND p.quantity <= p.minimumStock
            ORDER BY p.quantity ASC, p.id ASC
            """)
    List<Product> findLowStockProducts();

    @Query("""
            SELECT p
            FROM Product p
            WHERE p.active = true
              AND p.quantity = 0
            ORDER BY p.id ASC
            """)
    List<Product> findOutOfStockProducts();
}