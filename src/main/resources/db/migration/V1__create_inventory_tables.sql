

CREATE TABLE products (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    name VARCHAR(150) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    description TEXT,
    category VARCHAR(100),

    unit_price NUMERIC(19, 2) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    minimum_stock INTEGER NOT NULL DEFAULT 0,

    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_products_sku
        UNIQUE (sku),

    CONSTRAINT chk_products_name_not_blank
        CHECK (LENGTH(TRIM(name)) > 0),

    CONSTRAINT chk_products_sku_not_blank
        CHECK (LENGTH(TRIM(sku)) > 0),

    CONSTRAINT chk_products_unit_price_nonnegative
        CHECK (unit_price >= 0),

    CONSTRAINT chk_products_quantity_nonnegative
        CHECK (quantity >= 0),

    CONSTRAINT chk_products_minimum_stock_nonnegative
        CHECK (minimum_stock >= 0),

    CONSTRAINT chk_products_version_nonnegative
        CHECK (version >= 0)
);


CREATE TABLE inventory_movements (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    product_id BIGINT NOT NULL,
    movement_type VARCHAR(20) NOT NULL,

    quantity INTEGER NOT NULL,
    previous_stock INTEGER NOT NULL,
    new_stock INTEGER NOT NULL,

    reason VARCHAR(500) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inventory_movements_product
        FOREIGN KEY (product_id)
        REFERENCES products (id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_inventory_movements_type
        CHECK (
            movement_type IN (
                'INITIAL_STOCK',
                'STOCK_IN',
                'STOCK_OUT'
            )
        ),

    CONSTRAINT chk_inventory_movements_quantity_positive
        CHECK (quantity > 0),

    CONSTRAINT chk_inventory_movements_previous_stock_nonnegative
        CHECK (previous_stock >= 0),

    CONSTRAINT chk_inventory_movements_new_stock_nonnegative
        CHECK (new_stock >= 0),

    CONSTRAINT chk_inventory_movements_reason_not_blank
        CHECK (LENGTH(TRIM(reason)) > 0),

    CONSTRAINT chk_inventory_movements_stock_change
        CHECK (
            (
                movement_type = 'INITIAL_STOCK'
                AND previous_stock = 0
                AND new_stock = quantity
            )
            OR
            (
                movement_type = 'STOCK_IN'
                AND new_stock::BIGINT =
                    previous_stock::BIGINT + quantity::BIGINT
            )
            OR
            (
                movement_type = 'STOCK_OUT'
                AND new_stock::BIGINT =
                    previous_stock::BIGINT - quantity::BIGINT
            )
        )
);


CREATE INDEX idx_inventory_movements_product_history
    ON inventory_movements (
        product_id,
        created_at DESC,
        id DESC
    );
