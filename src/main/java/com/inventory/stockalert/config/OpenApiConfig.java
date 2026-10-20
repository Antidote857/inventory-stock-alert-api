package com.inventory.stockalert.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inventoryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Inventory & Stock Alert API")
                        .version("1.0")
                        .description("""
                                REST API for managing products, recording stock
                                movements, and identifying products needing restocking.

                                Product listings and stock alerts include active
                                products only. Inactive products remain accessible
                                by ID or SKU, and their movement history is retained.

                                Stock changes require a positive quantity and a reason.
                                Stock cannot become negative.

                                SKU matching is case-sensitive. Deactivated product
                                SKUs remain reserved.

                                Low stock means a positive quantity at or below the
                                minimum stock threshold. Zero quantity is out of stock.
                                """));
    }
}