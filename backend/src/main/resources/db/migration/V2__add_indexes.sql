-- Every index below targets a query pattern already in the codebase --
-- explained per-index so this is defensible in an interview, not a
-- blind "index everything" pass.

-- Student order history: findByStudentOrderByPlacedAtDesc
CREATE INDEX idx_orders_student_placed_at ON orders (student_id, placed_at DESC);

-- Staff kitchen queue + admin/staff status-filtered lookups
CREATE INDEX idx_orders_status_slot_start ON orders (status, pickup_slot_start);

-- Delay/no-show schedulers scan by status + a timestamp threshold
CREATE INDEX idx_orders_status_ready_at ON orders (status, ready_at);

-- Analytics: daily summary filters by placed_at range
CREATE INDEX idx_orders_placed_at ON orders (placed_at);

-- Menu browsing filters by category constantly
CREATE INDEX idx_menu_items_category ON menu_items (category_id);

-- Push notifications look up by user on every order-ready event
CREATE INDEX idx_push_subscriptions_user ON push_subscriptions (user_id);

-- Idempotency cleanup scheduler scans by expiry
CREATE INDEX idx_idempotency_keys_expires_at ON idempotency_keys (expires_at);

-- Analytics peak-hours query groups kitchen load snapshots by time
CREATE INDEX idx_kitchen_load_snapshots_created_at ON kitchen_load_snapshots (created_at);

-- Refresh token lookups by user (session listing) happen on every account page load
CREATE INDEX idx_refresh_tokens_user_revoked ON refresh_tokens (user_id, revoked);