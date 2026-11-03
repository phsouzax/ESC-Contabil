package com.jsb.recebimentos.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InstallmentTest {

    @Test
    void partialPaymentLeavesOnlyTheRemainingBalanceOpen() {
        Installment installment = installment("1050.00");

        installment.recordPayment(new BigDecimal("500.00"), LocalDate.now());

        assertEquals(new BigDecimal("500.00"), installment.getPaidAmount());
        assertEquals(new BigDecimal("550.00"), installment.getOpenAmount());
        assertEquals("A VENCER", installment.getStatus());
    }

    @Test
    void paymentEqualToOpenBalanceMarksInstallmentPaid() {
        Installment installment = installment("1050.00");

        installment.recordPayment(new BigDecimal("1050.00"), LocalDate.now());

        assertEquals(BigDecimal.ZERO.setScale(2), installment.getOpenAmount());
        assertEquals("PAGO", installment.getStatus());
    }

    @Test
    void rejectsPaymentAboveInstallmentAmountWithoutChangingBalance() {
        Installment installment = installment("1050.00");

        assertThrows(IllegalArgumentException.class,
                () -> installment.recordPayment(new BigDecimal("1050.01"), LocalDate.now()));

        assertEquals(BigDecimal.ZERO, installment.getPaidAmount());
        assertEquals(new BigDecimal("1050.00"), installment.getOpenAmount());
    }

    @Test
    void rejectsPaymentAboveRemainingBalanceAfterPartialPayment() {
        Installment installment = installment("1050.00");
        installment.recordPayment(new BigDecimal("500.00"), LocalDate.now());

        assertThrows(IllegalArgumentException.class,
                () -> installment.recordPayment(new BigDecimal("550.01"), LocalDate.now()));

        assertEquals(new BigDecimal("500.00"), installment.getPaidAmount());
        assertEquals(new BigDecimal("550.00"), installment.getOpenAmount());
    }

    @Test
    void rejectsZeroAndNegativePayments() {
        Installment installment = installment("1050.00");

        assertThrows(IllegalArgumentException.class,
                () -> installment.recordPayment(BigDecimal.ZERO, LocalDate.now()));
        assertThrows(IllegalArgumentException.class,
                () -> installment.recordPayment(new BigDecimal("-1.00"), LocalDate.now()));
    }

    @Test
    void missingAmountDefaultsToTheRemainingBalance() {
        Installment installment = installment("1050.00");

        installment.recordPayment(null, LocalDate.now());

        assertEquals("PAGO", installment.getStatus());
        assertEquals(new BigDecimal("1050.00"), installment.getPaidAmount());
    }

    private Installment installment(String amount) {
        LocalDate today = LocalDate.now();
        Invoice invoice = new Invoice("123", "Cliente", "12345678000199", "55", today,
                YearMonth.from(today).toString());
        invoice.addInstallment(1, new BigDecimal(amount), today.plusDays(10));
        return invoice.getInstallments().get(0);
    }
}
