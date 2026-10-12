package com.inventory.stockalert.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.stockalert.dto.InventoryMovementResponse;
import com.inventory.stockalert.dto.StockMovementRequest;
import com.inventory.stockalert.entity.InventoryMovement;
import com.inventory.stockalert.entity.Product;
import com.inventory.stockalert.enums.MovementType;
import com.inventory.stockalert.exception.ApiException;
import com.inventory.stockalert.repository.InventoryMovementRepository;
import com.inventory.stockalert.repository.ProductRepository;

@Service
@Transactional(readOnly = true)
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryMovementRepository movementRepository;

    public InventoryService(
            ProductRepository productRepository,
            InventoryMovementRepository movementRepository) {

        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional
    public InventoryMovementResponse addStock(
            Long productId,
            StockMovementRequest request) {

        Product product = findActiveProduct(productId);
        int amount = request.quantity();
        int previousStock = product.getQuantity();

        if (amount > Integer.MAX_VALUE - previousStock) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "STOCK_LIMIT_EXCEEDED",
                    "Resulting stock quantity exceeds the supported limit."
            );
        }

        product.addStock(amount);

        return saveMovement(
                product,
                MovementType.STOCK_IN,
                amount,
                previousStock,
                request.reason()
        );
    }

    @Transactional
    public InventoryMovementResponse removeStock(
            Long productId,
            StockMovementRequest request) {

        Product product = findActiveProduct(productId);
        int amount = request.quantity();
        int previousStock = product.getQuantity();

        if (amount > previousStock) {
            throw ApiException.insufficientStock(previousStock, amount);
        }

        product.removeStock(amount);

        return saveMovement(
                product,
                MovementType.STOCK_OUT,
                amount,
                previousStock,
                request.reason()
        );
    }

    public List<InventoryMovementResponse> getMovements(Long productId) {
        // Confirm the product exists, including inactive products.
        findProduct(productId);

        return movementRepository
                .findByProduct_IdOrderByCreatedAtDescIdDesc(productId)
                .stream()
                .map(InventoryMovementResponse::from)
                .toList();
    }

    private InventoryMovementResponse saveMovement(
            Product product,
            MovementType movementType,
            int amount,
            int previousStock,
            String reason) {

        // Flush detects version conflicts before recording the movement.
        // Both writes remain inside the caller's transaction.
        productRepository.saveAndFlush(product);

        InventoryMovement movement = new InventoryMovement(
                product,
                movementType,
                amount,
                previousStock,
                product.getQuantity(),
                reason
        );

        InventoryMovement savedMovement =
                movementRepository.saveAndFlush(movement);

        return InventoryMovementResponse.from(savedMovement);
    }

    private Product findActiveProduct(Long productId) {
        Product product = findProduct(productId);

        if (!product.isActive()) {
            throw ApiException.inactiveProduct(productId);
        }

        return product;
    }

    private Product findProduct(Long productId) {
        if (productId == null || productId <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PRODUCT_ID",
                    "Product ID must be greater than zero."
            );
        }

        return productRepository.findById(productId)
                .orElseThrow(() -> ApiException.productNotFound(productId));
    }
}