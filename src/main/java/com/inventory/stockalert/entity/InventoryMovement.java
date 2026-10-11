package com.inventory.stockalert.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.inventory.stockalert.enums.MovementType;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "movement_type",
            nullable = false,
            updatable = false,
            length = 20
    )
    private MovementType movementType;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Column(name = "previous_stock", nullable = false, updatable = false)
    private int previousStock;

    @Column(name = "new_stock", nullable = false, updatable = false)
    private int newStock;

    @Column(nullable = false, updatable = false, length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected InventoryMovement() {
        // Required by JPA.
    }

    public InventoryMovement(
            Product product,
            MovementType movementType,
            int quantity,
            int previousStock,
            int newStock,
            String reason) {

        if (product == null) {
            throw new IllegalArgumentException("Product is required.");
        }

        if (!product.isActive()) {
            throw new IllegalStateException(
                    "Inactive products cannot receive stock movements."
            );
        }

        if (movementType == null) {
            throw new IllegalArgumentException("Movement type is required.");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Movement quantity must be greater than zero."
            );
        }

        if (previousStock < 0 || newStock < 0) {
            throw new IllegalArgumentException(
                    "Stock quantities cannot be negative."
            );
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason cannot be blank.");
        }

        String trimmedReason = reason.trim();

        if (trimmedReason.length() > 500) {
            throw new IllegalArgumentException(
                    "Reason cannot exceed 500 characters."
            );
        }

        boolean validChange = switch (movementType) {
            case INITIAL_STOCK ->
                    previousStock == 0 && newStock == quantity;

            case STOCK_IN ->
                    (long) previousStock + quantity == newStock;

            case STOCK_OUT ->
                    (long) previousStock - quantity == newStock;
        };

        if (!validChange) {
            throw new IllegalArgumentException(
                    "Stock values do not match the movement type and quantity."
            );
        }

        if (product.getQuantity() != newStock) {
            throw new IllegalArgumentException(
                    "Product quantity must match the movement's new stock."
            );
        }

        this.product = product;
        this.movementType = movementType;
        this.quantity = quantity;
        this.previousStock = previousStock;
        this.newStock = newStock;
        this.reason = trimmedReason;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getPreviousStock() {
        return previousStock;
    }

    public int getNewStock() {
        return newStock;
    }

    public String getReason() {
        return reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}