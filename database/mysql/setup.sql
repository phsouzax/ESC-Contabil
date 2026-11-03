-- Execute uma vez com uma conta administrativa do MySQL local.
-- Troque SUBSTITUA_POR_UMA_SENHA_FORTE por uma senha exclusiva antes de executar.
CREATE DATABASE IF NOT EXISTS esc_contabil
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'esc_app'@'127.0.0.1'
    IDENTIFIED BY 'SUBSTITUA_POR_UMA_SENHA_FORTE';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
    ON esc_contabil.* TO 'esc_app'@'127.0.0.1';
