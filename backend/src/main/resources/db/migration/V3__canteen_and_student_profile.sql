-- Campus-specific configuration for the single college canteen.
CREATE TABLE canteen_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    canteen_name VARCHAR(150) NOT NULL,
    opening_time TIME NOT NULL,
    closing_time TIME NOT NULL,
    ordering_enabled BOOLEAN NOT NULL
) ENGINE=InnoDB;

CREATE TABLE canteen_closures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    closure_date DATE NOT NULL,
    reason VARCHAR(200) NOT NULL,
    UNIQUE KEY uk_canteen_closure_date (closure_date)
) ENGINE=InnoDB;

INSERT INTO canteen_settings (canteen_name, opening_time, closing_time, ordering_enabled)
VALUES ('Campus Main Canteen', '08:00:00', '18:00:00', TRUE);

ALTER TABLE users ADD COLUMN department VARCHAR(100) NULL;
ALTER TABLE users ADD COLUMN academic_year VARCHAR(30) NULL;
ALTER TABLE users ADD COLUMN section VARCHAR(30) NULL;
