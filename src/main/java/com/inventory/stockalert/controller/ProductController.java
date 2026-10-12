package com.inventory.stockalert.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.inventory.stockalert.dto.CreateProductRequest;
import com.inventory.stockalert.dto.ProductResponse;
import com.inventory.stockalert.dto.UpdateProductRequest;
import com.inventory.stockalert.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request) {

        ProductResponse product = productService.createProduct(request);

        URI location = URI.create("/api/products/" + product.id());

        return ResponseEntity.created(location).body(product);
    }

    @GetMapping
    public List<ProductResponse> getProducts(
            @RequestParam(name = "category", required = false)
            String category) {

        return productService.getProducts(category);
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(
            @PathVariable("id") Long id) {

        return productService.getProductById(id);
    }

    @GetMapping("/sku/{sku}")
    public ProductResponse getProductBySku(
            @PathVariable("sku") String sku) {

        return productService.getProductBySku(sku);
    }

    @GetMapping("/search")
    public List<ProductResponse> searchProducts(
            @RequestParam("name") String name) {

        return productService.searchProductsByName(name);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateProductRequest request) {

        return productService.updateProduct(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    public ProductResponse deactivateProduct(
            @PathVariable("id") Long id) {

        return productService.deactivateProduct(id);
    }
}