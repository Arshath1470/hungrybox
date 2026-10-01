CREATE TABLE invoices (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number    VARCHAR(40)    NOT NULL,
    order_id          BIGINT         NOT NULL,
    user_id           BIGINT         NOT NULL,
    restaurant_id     BIGINT         NOT NULL,
    subtotal          DECIMAL(10, 2) NOT NULL,
    delivery_fee      DECIMAL(10, 2) NOT NULL,
    tax               DECIMAL(10, 2) NOT NULL,
    total             DECIMAL(10, 2) NOT NULL,
    status            VARCHAR(20)    NOT NULL,
    payment_reference VARCHAR(60),
    refund_reference  VARCHAR(60),
    failure_reason    VARCHAR(255),
    created_at        TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_invoices_order UNIQUE (order_id),
    CONSTRAINT uk_invoices_number UNIQUE (invoice_number)
);

CREATE INDEX idx_invoices_user ON invoices (user_id);
