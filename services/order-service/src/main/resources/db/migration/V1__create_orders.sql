CREATE TABLE orders (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT         NOT NULL,
    restaurant_id    BIGINT         NOT NULL,
    status           VARCHAR(30)    NOT NULL,
    subtotal         DECIMAL(10, 2) NOT NULL,
    delivery_fee     DECIMAL(10, 2) NOT NULL,
    tax              DECIMAL(10, 2) NOT NULL,
    total            DECIMAL(10, 2) NOT NULL,
    delivery_address VARCHAR(255)   NOT NULL,
    invoice_number   VARCHAR(40),
    status_reason    VARCHAR(255),
    created_at       TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_orders_user ON orders (user_id);

CREATE TABLE order_items (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT         NOT NULL,
    menu_item_id BIGINT         NOT NULL,
    name         VARCHAR(120)   NOT NULL,
    unit_price   DECIMAL(10, 2) NOT NULL,
    quantity     INT            NOT NULL,
    line_total   DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
);
