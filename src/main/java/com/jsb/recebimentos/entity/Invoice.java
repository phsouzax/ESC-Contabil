package com.jsb.recebimentos.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoice", uniqueConstraints = @UniqueConstraint(columnNames = {"invoice_number", "customer_document"}))
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "invoice_number", nullable = false)
    private String number;
    private String referenceMonth;
    private String customer;
    private String customerDocument;
    private String model;
    private LocalDate issueDate;
    @Column(length = 1000000)
    private String originalXml;
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("installmentNumber ASC")
    private List<Installment> installments = new ArrayList<>();

    protected Invoice() {
    }

    public Invoice(String number, String customer, String customerDocument, String model, LocalDate issueDate, String referenceMonth) {
        this.number = number;
        this.customer = customer;
        this.customerDocument = customerDocument;
        this.model = model;
        this.issueDate = issueDate;
        this.referenceMonth = referenceMonth;
    }

    public void addInstallment(int number, BigDecimal amount, LocalDate dueDate) {
        Installment i = new Installment(this, number, amount, dueDate);
        installments.add(i);
    }

    public void updateDetails(String number, String customer, String customerDocument, String model,
                              LocalDate issueDate, String referenceMonth) {
        this.number = number;
        this.customer = customer;
        this.customerDocument = customerDocument;
        this.model = model;
        this.issueDate = issueDate;
        this.referenceMonth = referenceMonth;
    }

    public void removeInstallment(Installment installment) {
        installments.remove(installment);
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public String getReferenceMonth() {
        return referenceMonth;
    }

    public String getCustomer() {
        return customer;
    }

    public String getCustomerDocument() {
        return customerDocument;
    }

    public String getModel() {
        return model;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public List<Installment> getInstallments() {
        return installments;
    }

    public BigDecimal getTotal() {
        return installments.stream().map(Installment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getPaidTotal() {
        return installments.stream().map(Installment::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long getPaidInstallmentCount() {
        return installments.stream().filter(i -> i.getOpenAmount().signum() == 0).count();
    }

    public String getPaymentStatus() {
        if (getPaidTotal().signum() == 0) return "Sem recebimentos";
        return getPaidTotal().compareTo(getTotal()) >= 0 ? "Nota paga" : "Nota parcialmente paga";
    }

    public String getOriginalXml() {
        return originalXml;
    }

    public void setOriginalXml(String xml) {
        this.originalXml = xml;
    }
}
