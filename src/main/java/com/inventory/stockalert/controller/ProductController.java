package com.inventory.stockalert.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.inventory.stockalert.dto.ApiError;
import com.inventory.stockalert.dto.CreateProductRequest;
import com.inventory.stockalert.dto.ProductResponse;
import com.inventory.stockalert.dto.UpdateProductRequest;
import com.inventory.stockalert.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
@Tag(
        name = "Products",
        description = "Register, retrieve, update, and deactivate products."
)
@ApiResponse(
        responseCode = "400",
        description = "Invalid request body, field value, or parameter.",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
        )
)
@ApiResponse(
        responseCode = "500",
        description = "Unexpected server error.",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
        )
)
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @Operation(
            summary = "Register a product",
            description = """
                    Creates an active product with a unique, case-sensitive SKU.
                    Positive initial stock creates an INITIAL_STOCK movement
                    in the same transaction. Zero initial stock creates no movement.
                    SKUs belonging to inactive products remain reserved.
                    """,
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            implementation = CreateProductRequest.class
                                    ),
                                    examples = @ExampleObject(
                                            name = "Wireless keyboard",
                                            value = """
                                                    {
                                                      "name": "Wireless Keyboard",
                                                      "sku": "KEYBOARD-001",
                                                      "description": "Compact wireless keyboard",
                                                      "category": "Computer Accessories",
                                                      "unitPrice": 399.99,
                                                      "quantity": 20,
                                                      "minimumStock": 5
                                                    }
                                                    """
                                    )
                            )
                    )
    )
    @ApiResponse(
            responseCode = "201",
            description = "Product registered successfully.",
            headers = @Header(
                    name = "Location",
                    description = "Relative URL of the created product.",
                    schema = @Schema(
                            type = "string",
                            example = "/api/products/2"
                    )
            ),
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "DUPLICATE_SKU or another database data conflict.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiError.class)
            )
    )
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request) {

        ProductResponse product = productService.createProduct(request);
        URI location = URI.create("/api/products/" + product.id());

        return ResponseEntity.created(location).body(product);
    }

    @GetMapping
    @Operation(
            summary = "List active products",
            description = """
                    Returns active products ordered by ID.
                    Optionally filters by an exact, case-insensitive category.
                    Returns an empty array when no products match.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Matching active products.",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(
                            schema = @Schema(implementation = ProductResponse.class)
                    )
            )
    )
    public List<ProductResponse> getProducts(
            @Parameter(
                    description = "Optional category; must not be blank if supplied.",
                    example = "Computer Accessories"
            )
            @RequestParam(name = "category", required = false)
            String category) {

        return productService.getProducts(category);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Retrieve a product by ID",
            description = "Returns an active or inactive product."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Product found.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "PRODUCT_NOT_FOUND.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiError.class)
            )
    )
    public ProductResponse getProductById(
            @Parameter(description = "Positive product ID.", example = "1")
            @PathVariable("id") Long id) {

        return productService.getProductById(id);
    }

    @GetMapping("/sku/{sku}")
    @Operation(
            summary = "Retrieve a product by SKU",
            description = """
                    Uses an exact, case-sensitive SKU match.
                    Includes inactive products.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Product found.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "PRODUCT_NOT_FOUND.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiError.class)
            )
    )
    public ProductResponse getProductBySku(
            @Parameter(example = "MOUSE-001")
            @PathVariable("sku") String sku) {

        return productService.getProductBySku(sku);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search active products by name",
            description = """
                    Performs a case-insensitive partial name search.
                    Returns matches ordered by ID, or an empty array.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Matching active products.",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(
                            schema = @Schema(implementation = ProductResponse.class)
                    )
            )
    )
    public List<ProductResponse> searchProducts(
            @Parameter(
                    description = "Nonblank name or part of a name.",
                    example = "mouse"
            )
            @RequestParam("name") String name) {

        return productService.searchProductsByName(name);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update product details",
            description = """
                    Updates name, description, category, unit price, and minimum stock.
                    SKU and stock quantity remain unchanged.
                    Name, unit price, and minimum stock are required.
                    Omitting description or category clears those values.
                    Details may also be updated for inactive products.
                    """,
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            implementation = UpdateProductRequest.class
                                    ),
                                    examples = @ExampleObject(
                                            name = "Updated keyboard details",
                                            value = """
                                                    {
                                                      "name": "Compact Wireless Keyboard",
                                                      "description": "Updated keyboard model",
                                                      "category": "Computer Accessories",
                                                      "unitPrice": 429.99,
                                                      "minimumStock": 8
                                                    }
                                                    """
                                    )
                            )
                    )
    )
    @ApiResponse(
            responseCode = "200",
            description = "Updated product.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "PRODUCT_NOT_FOUND.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiError.class)
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "CONCURRENT_UPDATE or another database data conflict.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiError.class)
            )
    )
    public ProductResponse updateProduct(
            @Parameter(description = "Positive product ID.", example = "2")
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateProductRequest request) {

        return productService.updateProduct(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(
            summary = "Deactivate a product",
            description = """
                    Marks the product inactive without deleting it or its history.
                    Inactive products are excluded from listings, alerts, and summaries,
                    and cannot receive stock movements. Their SKUs remain reserved.
                    Repeating this operation leaves the product inactive.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Product with active set to false.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "PRODUCT_NOT_FOUND.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiError.class)
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "CONCURRENT_UPDATE or another database data conflict.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiError.class)
            )
    )
    public ProductResponse deactivateProduct(
            @Parameter(description = "Positive product ID.", example = "2")
            @PathVariable("id") Long id) {

        return productService.deactivateProduct(id);
    }
}