-- Auditability and integrity improvements.
UPDATE orders SET version = 0 WHERE version IS NULL;
ALTER TABLE orders MODIFY version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE wallet_transactions ADD COLUMN order_id BIGINT NULL;
ALTER TABLE wallet_transactions
    ADD CONSTRAINT fk_wallet_transactions_order FOREIGN KEY (order_id) REFERENCES orders(id),
    ADD INDEX idx_wallet_transactions_order (order_id),
    ADD INDEX idx_wallet_transactions_wallet_created (wallet_id, created_at);
