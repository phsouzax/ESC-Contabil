CREATE TABLE app_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(80) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    totp_secret VARCHAR(128) NOT NULL,
    recovery_code_hashes TEXT NOT NULL,
    last_mfa_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_user_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
