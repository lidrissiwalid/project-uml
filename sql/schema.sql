-- StadiumEats Database Schema
-- MySQL 8.0

CREATE DATABASE IF NOT EXISTS stadiumeats
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE stadiumeats;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)     NOT NULL UNIQUE,
    email         VARCHAR(100)    NOT NULL UNIQUE,
    password_hash VARCHAR(60)     NOT NULL,
    role          ENUM('CLIENT','WORKER','ADMIN') NOT NULL DEFAULT 'CLIENT',
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Menu items table
CREATE TABLE IF NOT EXISTS menu_items (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)    NOT NULL,
    description TEXT,
    price       DECIMAL(10, 2)  NOT NULL,
    category    VARCHAR(50)     NOT NULL,
    image_url   VARCHAR(500),
    available   BOOLEAN         NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    client_id       BIGINT          NOT NULL,
    seat_number     VARCHAR(20)     NOT NULL,
    status          ENUM('PENDING','CONFIRMED','IN_DELIVERY','DELIVERED','CANCELLED')
                    NOT NULL DEFAULT 'PENDING',
    total_price     DECIMAL(10, 2)  NOT NULL,
    payment_method  VARCHAR(20)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_orders_client FOREIGN KEY (client_id)
        REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Order items table
CREATE TABLE IF NOT EXISTS order_items (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    order_id        BIGINT          NOT NULL,
    menu_item_id    BIGINT          NOT NULL,
    quantity        INT             NOT NULL CHECK (quantity > 0),
    unit_price      DECIMAL(10, 2)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_menu FOREIGN KEY (menu_item_id)
        REFERENCES menu_items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
