package com.inventory.stockalert.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.inventory.stockalert.enums.StockStatus;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String sku;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String category;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "minimum_stock", nullable = false)
    private int minimumStock;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Product() {
        // Required by JPA.
    }

    public Product(
            String name,
            String sku,
            String description,
            String category,
            BigDecimal unitPrice,
            int quantity,
            int minimumStock) {

        if (quantity < 0) {
            throw new IllegalArgumentException(
                    "Stock quantity cannot be negative."
            );
        }

        this.sku = requiredText(sku, "SKU", 64);
        updateDetails(name, description, category, unitPrice, minimumStock);
        this.quantity = quantity;
    }

    public void updateDetails(
            String name,
            String description,
            String category,
            BigDecimal unitPrice,
            int minimumStock) {

        String validatedName = requiredText(name, "Product name", 150);
        String validatedCategory = optionalText(category);

        if (validatedCategory != null && validatedCategory.length() > 100) {
            throw new IllegalArgumentException(
                    "Category cannot exceed 100 characters."
            );
        }

        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException(
                    "Unit price is required and cannot be negative."
            );
        }

        BigDecimal validatedPrice;

        try {
            validatedPrice = unitPrice.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Unit price cannot have more than two decimal places."
            );
        }

        if (validatedPrice.precision() > 19) {
            throw new IllegalArgumentException("Unit price is too large.");
        }

        if (minimumStock < 0) {
            throw new IllegalArgumentException(
                    "Minimum stock cannot be negative."
            );
        }

        this.name = validatedName;
        this.description = optionalText(description);
        this.category = validatedCategory;
        this.unitPrice = validatedPrice;
        this.minimumStock = minimumStock;
    }

    public void addStock(int amount) {
        requireActive();
        requirePositiveAmount(amount);

        if (amount > Integer.MAX_VALUE - quantity) {
            throw new IllegalArgumentException(
                    "Resulting stock quantity exceeds the supported limit."
            );
        }

        quantity += amount;
    }

    public void removeStock(int amount) {
        requireActive();
        requirePositiveAmount(amount);

        if (amount > quantity) {
            throw new IllegalStateException("Insufficient stock.");
        }

        quantity -= amount;
    }

    public void deactivate() {
        active = false;
    }

    @Transient
    public StockStatus getStockStatus() {
        if (quantity == 0) {
            return StockStatus.OUT_OF_STOCK;
        }

        if (quantity <= minimumStock) {
            return StockStatus.LOW_STOCK;
        }

        return StockStatus.IN_STOCK;
    }

    private void requireActive() {
        if (!active) {
            throw new IllegalStateException(
                    "Inactive products cannot receive stock movements."
            );
        }
    }

    private static void requirePositiveAmount(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Movement quantity must be greater than zero."
            );
        }
    }

    private static String requiredText(
            String value,
            String field,
            int maximumLength) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank.");
        }

        String trimmed = value.trim();

        if (trimmed.length() > maximumLength) {
            throw new IllegalArgumentException(
                    field + " cannot exceed " + maximumLength + " characters."
            );
        }

        return trimmed;
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSku() {
        return sku;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getMinimumStock() {
        return minimumStock;
    }

    public boolean isActive() {
        return active;
    }

    public Long getVersion() {
        return version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}