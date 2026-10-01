CREATE TABLE restaurants (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    name               VARCHAR(120)  NOT NULL,
    cuisine            VARCHAR(60)   NOT NULL,
    city               VARCHAR(60)   NOT NULL,
    rating             DECIMAL(2, 1) DEFAULT 4.0 NOT NULL,
    delivery_time_mins INT           DEFAULT 30 NOT NULL,
    is_open            BOOLEAN       DEFAULT TRUE NOT NULL,
    created_at         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE menu_items (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT         NOT NULL,
    name          VARCHAR(120)   NOT NULL,
    description   VARCHAR(255),
    category      VARCHAR(60)    NOT NULL,
    price         DECIMAL(10, 2) NOT NULL,
    is_veg        BOOLEAN        DEFAULT TRUE NOT NULL,
    is_available  BOOLEAN        DEFAULT TRUE NOT NULL,
    CONSTRAINT fk_menu_items_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id)
);
