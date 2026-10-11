package com.inventory.stockalert.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventory.stockalert.entity.InventoryMovement;

public interface InventoryMovementRepository
        extends JpaRepository<InventoryMovement, Long> {

    // Newest movements first; ID breaks ties between equal timestamps.
    // Includes history belonging to inactive products.
    List<InventoryMovement> findByProduct_IdOrderByCreatedAtDescIdDesc(
            Long productId
    );
}