CREATE TABLE invoice (
    id BIGINT NOT NULL AUTO_INCREMENT,
    invoice_number VARCHAR(255) NOT NULL,
    reference_month VARCHAR(255),
    customer VARCHAR(255),
    customer_document VARCHAR(255),
    model VARCHAR(255),
    issue_date DATE,
    original_xml LONGTEXT,
    PRIMARY KEY (id),
    CONSTRAINT uk_invoice_number_customer_document UNIQUE (invoice_number, customer_document)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE installment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    invoice_id BIGINT NOT NULL,
    installment_number INT NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    due_date DATE NOT NULL,
    paid_date DATE,
    paid_amount DECIMAL(14, 2),
    PRIMARY KEY (id),
    INDEX idx_installment_invoice_id (invoice_id),
    CONSTRAINT fk_installment_invoice FOREIGN KEY (invoice_id) REFERENCES invoice (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
