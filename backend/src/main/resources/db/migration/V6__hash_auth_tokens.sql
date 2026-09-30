-- Store opaque authentication tokens only as SHA-256 hashes.
ALTER TABLE refresh_tokens ADD COLUMN token_hash VARCHAR(64) NULL;
ALTER TABLE refresh_tokens ADD COLUMN replaced_by_token_hash VARCHAR(64) NULL;

UPDATE refresh_tokens
SET token_hash = SHA2(token, 256)
WHERE token_hash IS NULL;

UPDATE refresh_tokens
SET replaced_by_token_hash = SHA2(replaced_by_token, 256)
WHERE replaced_by_token IS NOT NULL
  AND replaced_by_token_hash IS NULL;

ALTER TABLE refresh_tokens MODIFY token_hash VARCHAR(64) NOT NULL;
ALTER TABLE refresh_tokens ADD UNIQUE KEY uk_refresh_tokens_token_hash (token_hash);

ALTER TABLE refresh_tokens DROP COLUMN token;
ALTER TABLE refresh_tokens DROP COLUMN replaced_by_token;


ALTER TABLE password_reset_tokens ADD COLUMN token_hash VARCHAR(64) NULL;

UPDATE password_reset_tokens
SET token_hash = SHA2(token, 256)
WHERE token_hash IS NULL;

ALTER TABLE password_reset_tokens MODIFY token_hash VARCHAR(64) NOT NULL;
ALTER TABLE password_reset_tokens
    ADD UNIQUE KEY uk_password_reset_tokens_token_hash (token_hash);

ALTER TABLE password_reset_tokens DROP COLUMN token;


ALTER TABLE email_verification_tokens ADD COLUMN token_hash VARCHAR(64) NULL;

UPDATE email_verification_tokens
SET token_hash = SHA2(token, 256)
WHERE token_hash IS NULL;

ALTER TABLE email_verification_tokens MODIFY token_hash VARCHAR(64) NOT NULL;
ALTER TABLE email_verification_tokens
    ADD UNIQUE KEY uk_email_verification_tokens_token_hash (token_hash);

ALTER TABLE email_verification_tokens DROP COLUMN token;