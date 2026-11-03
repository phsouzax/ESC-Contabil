package com.jsb.recebimentos.repository;

import com.jsb.recebimentos.entity.Invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByNumberAndCustomerDocument(String number, String document);
}
