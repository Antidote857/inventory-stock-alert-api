package com.inventory.stockalert.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.stockalert.dto.CreateProductRequest;
import com.inventory.stockalert.dto.ProductResponse;
import com.inventory.stockalert.dto.UpdateProductRequest;
import com.inventory.stockalert.entity.InventoryMovement;
import com.inventory.stockalert.entity.Product;
import com.inventory.stockalert.enums.MovementType;
import com.inventory.stockalert.exception.ApiException;
import com.inventory.stockalert.repository.InventoryMovementRepository;
import com.inventory.stockalert.repository.ProductRepository;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryMovementRepository movementRepository;

    public ProductService(
            ProductRepository productRepository,
            InventoryMovementRepository movementRepository) {

        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        String sku = requireText(request.sku(), "SKU");

        // Inactive products also retain their SKUs.
        if (productRepository.existsBySku(sku)) {
            throw ApiException.duplicateSku(sku);
        }

        Product product = new Product(
                request.name(),
                sku,
                request.description(),
                request.category(),
                request.unitPrice(),
                request.quantity(),
                request.minimumStock()
        );

        Product savedProduct = productRepository.saveAndFlush(product);

        // Zero initial stock does not create a movement.
        if (savedProduct.getQuantity() > 0) {
            InventoryMovement initialMovement = new InventoryMovement(
                    savedProduct,
                    MovementType.INITIAL_STOCK,
                    savedProduct.getQuantity(),
                    0,
                    savedProduct.getQuantity(),
                    "Initial stock recorded during product registration."
            );

            movementRepository.saveAndFlush(initialMovement);
        }

        return ProductResponse.from(savedProduct);
    }

    public List<ProductResponse> getProducts(String category) {
        List<Product> products;

        if (category == null) {
            products = productRepository.findByActiveTrueOrderByIdAsc();
        } else {
            String searchCategory = requireText(category, "Category");

            products = productRepository
                    .findByActiveTrueAndCategoryIgnoreCaseOrderByIdAsc(
                            searchCategory
                    );
        }

        return products.stream()
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse getProductById(Long productId) {
        return ProductResponse.from(findProduct(productId));
    }

    public ProductResponse getProductBySku(String sku) {
        String searchSku = requireText(sku, "SKU");

        Product product = productRepository.findBySku(searchSku)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "PRODUCT_NOT_FOUND",
                        "Product not found with SKU: " + searchSku
                ));

        return ProductResponse.from(product);
    }

    public List<ProductResponse> searchProductsByName(String name) {
        String searchName = requireText(name, "Search name");

        return productRepository
                .findByActiveTrueAndNameContainingIgnoreCaseOrderByIdAsc(
                        searchName
                )
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
    public ProductResponse updateProduct(
            Long productId,
            UpdateProductRequest request) {

        Product product = findProduct(productId);

        // Ordinary updates never change SKU or stock quantity.
        product.updateDetails(
                request.name(),
                request.description(),
                request.category(),
                request.unitPrice(),
                request.minimumStock()
        );

        Product savedProduct = productRepository.saveAndFlush(product);

        return ProductResponse.from(savedProduct);
    }

    @Transactional
    public ProductResponse deactivateProduct(Long productId) {
        Product product = findProduct(productId);

        // Repeating this operation leaves the product inactive.
        if (product.isActive()) {
            product.deactivate();
            productRepository.saveAndFlush(product);
        }

        return ProductResponse.from(product);
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

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    fieldName + " cannot be blank."
            );
        }

        return value.trim();
    }
}