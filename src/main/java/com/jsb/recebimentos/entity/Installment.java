package com.jsb.recebimentos.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "installment")
public class Installment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "paid_amount", precision = 14, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    protected Installment() {
    }

    public Installment(Invoice invoice, int installmentNumber, BigDecimal amount, LocalDate dueDate) {
        this.invoice = invoice;
        this.installmentNumber = installmentNumber;
        this.amount = amount;
        this.dueDate = dueDate;
    }



    public Long getId() {
        return id;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public int getInstallmentNumber() {
        return installmentNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getPaidDate() {
        return paidDate;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount == null ? BigDecimal.ZERO : paidAmount;
    }



    public BigDecimal getOpenAmount() {
        return amount.subtract(getPaidAmount()).max(BigDecimal.ZERO);
    }

    public String getStatus() {
        if (getOpenAmount().signum() == 0) {
            return "PAGO";
        }
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            return "VENCIDO";
        }
        return "A VENCER";
    }

    public long getDaysToDue() {
        return dueDate == null ? 0 : ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    public String getReceivedMonth() {
        return paidDate == null ? "" : paidDate.toString().substring(0, 7);
    }

    public void recordPayment(BigDecimal value, LocalDate date) {
        BigDecimal received = (value == null) ? getOpenAmount() : value;

        if (received.signum() <= 0) {
            throw new IllegalArgumentException("Informe um valor recebido maior que zero");
        }
        if (received.compareTo(getOpenAmount()) > 0) {
            throw new IllegalArgumentException("O valor recebido não pode ser maior que o saldo em aberto da parcela (" + getOpenAmount().toPlainString() + ").");
        }

        this.paidAmount = getPaidAmount().add(received);
        this.paidDate = (date == null) ? LocalDate.now() : date;
    }

    public void updateDetails(BigDecimal amount, LocalDate dueDate, BigDecimal paidAmount, LocalDate paidDate) {
        this.amount = amount;
        this.dueDate = dueDate;
        this.paidAmount = paidAmount;
        this.paidDate = paidAmount.signum() == 0 ? null : (paidDate == null ? LocalDate.now() : paidDate);
    }
}
