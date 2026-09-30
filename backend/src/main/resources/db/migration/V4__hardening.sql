-- Final hardening: cancellation reasons, integrity constraints and query indexes.
ALTER TABLE orders ADD COLUMN cancel_reason VARCHAR(300) NULL;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_total_amount_nonnegative CHECK (total_amount >= 0),
    ADD INDEX idx_orders_pickup_slot_end (pickup_slot_end);

ALTER TABLE order_items
    ADD CONSTRAINT chk_order_items_quantity_positive CHECK (quantity > 0);

ALTER TABLE cart_items
    ADD CONSTRAINT chk_cart_items_quantity_positive CHECK (quantity > 0);

ALTER TABLE slot_capacities
    ADD CONSTRAINT uk_slot_capacities_window_start UNIQUE (window_start_minute_of_day),
    ADD CONSTRAINT chk_slot_capacities_max_orders_positive CHECK (max_orders > 0);

ALTER TABLE slot_reservations
    ADD CONSTRAINT chk_slot_reservations_reserved_count_nonnegative CHECK (reserved_count >= 0);

ALTER TABLE wallets
    ADD CONSTRAINT chk_wallets_balance_nonnegative CHECK (balance >= 0);

ALTER TABLE wallet_transactions
    ADD CONSTRAINT chk_wallet_transactions_balance_nonnegative CHECK (balance_after >= 0);
