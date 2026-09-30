-- V1: initial CampusBite schema.
-- Fresh databases execute this migration first, followed by V2 and V3.
-- Keep ddl-auto=validate so Hibernate checks the migration-managed schema
-- instead of creating or mutating tables at runtime.

CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       created_at DATETIME(6),
                       updated_at DATETIME(6),
                       full_name VARCHAR(255) NOT NULL,
                       email VARCHAR(255) NOT NULL,
                       login_id VARCHAR(255) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(20) NOT NULL,
                       active BOOLEAN NOT NULL,
                       email_verified BOOLEAN NOT NULL,
                       payment_mode VARCHAR(20) NOT NULL,
                       UNIQUE KEY uk_users_email (email),
                       UNIQUE KEY uk_users_login_id (login_id)
) ENGINE=InnoDB;

CREATE TABLE refresh_tokens (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                created_at DATETIME(6),
                                updated_at DATETIME(6),
                                token VARCHAR(64) NOT NULL,
                                user_id BIGINT NOT NULL,
                                expires_at DATETIME(6) NOT NULL,
                                revoked BOOLEAN NOT NULL,
                                replaced_by_token VARCHAR(255),
                                user_agent VARCHAR(500),
                                UNIQUE KEY uk_refresh_tokens_token (token),
                                CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE email_verification_tokens (
                                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           created_at DATETIME(6),
                                           updated_at DATETIME(6),
                                           token VARCHAR(64) NOT NULL,
                                           user_id BIGINT NOT NULL,
                                           expires_at DATETIME(6) NOT NULL,
                                           used BOOLEAN NOT NULL,
                                           UNIQUE KEY uk_email_verification_tokens_token (token),
                                           CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE password_reset_tokens (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       created_at DATETIME(6),
                                       updated_at DATETIME(6),
                                       token VARCHAR(64) NOT NULL,
                                       user_id BIGINT NOT NULL,
                                       expires_at DATETIME(6) NOT NULL,
                                       used BOOLEAN NOT NULL,
                                       UNIQUE KEY uk_password_reset_tokens_token (token),
                                       CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE categories (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            created_at DATETIME(6),
                            updated_at DATETIME(6),
                            name VARCHAR(255) NOT NULL,
                            display_order INT NOT NULL,
                            available_from_minute_of_day INT,
                            available_to_minute_of_day INT,
                            UNIQUE KEY uk_categories_name (name)
) ENGINE=InnoDB;

CREATE TABLE menu_items (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            created_at DATETIME(6),
                            updated_at DATETIME(6),
                            name VARCHAR(255) NOT NULL,
                            description VARCHAR(500),
                            price DECIMAL(10,2) NOT NULL,
                            category_id BIGINT NOT NULL,
                            image_url VARCHAR(500),
                            available BOOLEAN NOT NULL,
                            daily_special BOOLEAN NOT NULL,
                            baseline_prep_time_minutes INT NOT NULL,
                            diet_type VARCHAR(20) NOT NULL,
                            CONSTRAINT fk_menu_items_category FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB;

CREATE TABLE menu_item_customizations (
                                          menu_item_id BIGINT NOT NULL,
                                          option_label VARCHAR(255),
                                          CONSTRAINT fk_menu_item_customizations_item FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
) ENGINE=InnoDB;

CREATE TABLE menu_item_allergens (
                                     menu_item_id BIGINT NOT NULL,
                                     allergen VARCHAR(255),
                                     CONSTRAINT fk_menu_item_allergens_item FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
) ENGINE=InnoDB;

CREATE TABLE carts (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       created_at DATETIME(6),
                       updated_at DATETIME(6),
                       student_id BIGINT NOT NULL,
                       UNIQUE KEY uk_carts_student (student_id),
                       CONSTRAINT fk_carts_student FOREIGN KEY (student_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE cart_items (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            created_at DATETIME(6),
                            updated_at DATETIME(6),
                            cart_id BIGINT NOT NULL,
                            menu_item_id BIGINT NOT NULL,
                            quantity INT NOT NULL,
                            CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id),
                            CONSTRAINT fk_cart_items_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
) ENGINE=InnoDB;

CREATE TABLE cart_item_customizations (
                                          cart_item_id BIGINT NOT NULL,
                                          option_label VARCHAR(255),
                                          CONSTRAINT fk_cart_item_customizations_item FOREIGN KEY (cart_item_id) REFERENCES cart_items(id)
) ENGINE=InnoDB;

CREATE TABLE orders (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        created_at DATETIME(6),
                        updated_at DATETIME(6),
                        student_id BIGINT NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        total_amount DECIMAL(10,2) NOT NULL,
                        pickup_token VARCHAR(255) NOT NULL,
                        pickup_slot_start DATETIME(6) NOT NULL,
                        pickup_slot_end DATETIME(6) NOT NULL,
                        estimated_prep_minutes INT NOT NULL,
                        actual_prep_minutes INT,
                        special_instructions VARCHAR(300),
                        placed_at DATETIME(6) NOT NULL,
                        confirmed_at DATETIME(6),
                        preparing_at DATETIME(6),
                        ready_at DATETIME(6),
                        picked_up_at DATETIME(6),
                        cancelled_at DATETIME(6),
                        delay_notified BOOLEAN NOT NULL,
                        version BIGINT,
                        UNIQUE KEY uk_orders_pickup_token (pickup_token),
                        CONSTRAINT fk_orders_student FOREIGN KEY (student_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE order_items (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             created_at DATETIME(6),
                             updated_at DATETIME(6),
                             order_id BIGINT NOT NULL,
                             menu_item_id BIGINT NOT NULL,
                             menu_item_name_snapshot VARCHAR(255) NOT NULL,
                             quantity INT NOT NULL,
                             unit_price DECIMAL(10,2) NOT NULL,
                             item_index INT,
                             CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
                             CONSTRAINT fk_order_items_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
) ENGINE=InnoDB;

CREATE TABLE order_item_customizations (
                                           order_item_id BIGINT NOT NULL,
                                           option_label VARCHAR(255),
                                           CONSTRAINT fk_order_item_customizations_item FOREIGN KEY (order_item_id) REFERENCES order_items(id)
) ENGINE=InnoDB;

CREATE TABLE slot_capacities (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 created_at DATETIME(6),
                                 updated_at DATETIME(6),
                                 window_start_minute_of_day INT NOT NULL,
                                 max_orders INT NOT NULL
) ENGINE=InnoDB;

CREATE TABLE slot_reservations (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   created_at DATETIME(6),
                                   updated_at DATETIME(6),
                                   window_start DATETIME(6) NOT NULL,
                                   reserved_count INT NOT NULL,
                                   UNIQUE KEY uk_slot_reservations_window_start (window_start)
) ENGINE=InnoDB;

CREATE TABLE kitchen_load_snapshots (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        created_at DATETIME(6),
                                        updated_at DATETIME(6),
                                        active_order_count INT NOT NULL,
                                        backlog_minutes INT NOT NULL,
                                        load_percentage INT NOT NULL,
                                        level VARCHAR(20) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE trust_scores (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              created_at DATETIME(6),
                              updated_at DATETIME(6),
                              user_id BIGINT NOT NULL,
                              score INT NOT NULL,
                              total_orders INT NOT NULL,
                              no_show_count INT NOT NULL,
                              cancelled_late_count INT NOT NULL,
                              UNIQUE KEY uk_trust_scores_user (user_id),
                              CONSTRAINT fk_trust_scores_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE wallets (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         created_at DATETIME(6),
                         updated_at DATETIME(6),
                         student_id BIGINT NOT NULL,
                         balance DECIMAL(10,2) NOT NULL,
                         UNIQUE KEY uk_wallets_student (student_id),
                         CONSTRAINT fk_wallets_student FOREIGN KEY (student_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE wallet_transactions (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     created_at DATETIME(6),
                                     updated_at DATETIME(6),
                                     wallet_id BIGINT NOT NULL,
                                     type VARCHAR(30) NOT NULL,
                                     amount DECIMAL(10,2) NOT NULL,
                                     balance_after DECIMAL(10,2) NOT NULL,
                                     note VARCHAR(500),
                                     CONSTRAINT fk_wallet_transactions_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id)
) ENGINE=InnoDB;

CREATE TABLE push_subscriptions (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    created_at DATETIME(6),
                                    updated_at DATETIME(6),
                                    user_id BIGINT NOT NULL,
                                    endpoint VARCHAR(500) NOT NULL,
                                    p256dh VARCHAR(200) NOT NULL,
                                    auth VARCHAR(200) NOT NULL,
                                    UNIQUE KEY uk_push_subscriptions_endpoint (endpoint),
                                    CONSTRAINT fk_push_subscriptions_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE ratings (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         created_at DATETIME(6),
                         updated_at DATETIME(6),
                         order_id BIGINT NOT NULL,
                         stars INT NOT NULL,
                         comment VARCHAR(500),
                         UNIQUE KEY uk_ratings_order (order_id),
                         CONSTRAINT fk_ratings_order FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB;

CREATE TABLE broadcast_messages (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    created_at DATETIME(6),
                                    updated_at DATETIME(6),
                                    message VARCHAR(300) NOT NULL,
                                    active BOOLEAN NOT NULL
) ENGINE=InnoDB;

CREATE TABLE monthly_statements (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    created_at DATETIME(6),
                                    updated_at DATETIME(6),
                                    user_id BIGINT NOT NULL,
                                    year INT NOT NULL,
                                    month INT NOT NULL,
                                    order_count INT NOT NULL,
                                    total_amount DECIMAL(10,2) NOT NULL,
                                    settled BOOLEAN NOT NULL,
                                    settled_at DATETIME(6),
                                    UNIQUE KEY uk_monthly_statements_user_period (user_id, year, month),
                                    CONSTRAINT fk_monthly_statements_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE idempotency_keys (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  created_at DATETIME(6),
                                  updated_at DATETIME(6),
                                  idempotency_key VARCHAR(200) NOT NULL,
                                  user_id BIGINT NOT NULL,
                                  request_hash VARCHAR(128) NOT NULL,
                                  order_id BIGINT,
                                  expires_at DATETIME(6) NOT NULL,
                                  UNIQUE KEY uk_idempotency_keys_key_user (idempotency_key, user_id)
) ENGINE=InnoDB;