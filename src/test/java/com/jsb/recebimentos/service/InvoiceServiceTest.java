package com.jsb.recebimentos.service;

import com.jsb.recebimentos.entity.Installment;
import com.jsb.recebimentos.entity.Invoice;
import com.jsb.recebimentos.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository repository;

    private InvoiceService service;

    @BeforeEach
    void setUp() {
        service = new InvoiceService(repository);
    }

    @Test
    void createsManualInvoiceWithoutAnyPayment() {
        LocalDate issueDate = LocalDate.of(2026, 11, 3);
        LocalDate dueDate = issueDate.plusDays(30);
        when(repository.findByNumberAndCustomerDocument("123", "12345678000199"))
                .thenReturn(Optional.empty());

        service.createManual("123", "Cliente", "12.345.678/0001-99", "55", issueDate,
                List.of("1"), List.of("1050.00"), List.of(dueDate.toString()), null, null);

        ArgumentCaptor<Invoice> saved = ArgumentCaptor.forClass(Invoice.class);
        verify(repository).save(saved.capture());
        Invoice invoice = saved.getValue();
        Installment installment = invoice.getInstallments().get(0);
        assertEquals("2026-11", invoice.getReferenceMonth());
        assertEquals(BigDecimal.ZERO, installment.getPaidAmount());
        assertNull(installment.getPaidDate());
        assertEquals(new BigDecimal("1050.00"), installment.getOpenAmount());
    }

    @Test
    void rejectsManualInvoiceDuplicateByNumberAndCustomerDocument() {
        Invoice existing = invoice("123", "12345678000199");
        when(repository.findByNumberAndCustomerDocument("123", "12345678000199"))
                .thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> service.createManual("123", "Cliente",
                "12345678000199", "55", LocalDate.now(), List.of("1"), List.of("100.00"),
                List.of(LocalDate.now().plusDays(10).toString()), null, null));

        verify(repository, never()).save(any(Invoice.class));
    }

    @Test
    void updatesInstallmentsAndRemovesRowsOmittedFromEdit() {
        Invoice existing = invoice("123", "12345678000199");
        ReflectionTestUtils.setField(existing, "id", 77L);
        existing.addInstallment(1, new BigDecimal("1050.00"), LocalDate.of(2026, 11, 20));
        existing.addInstallment(2, new BigDecimal("1050.00"), LocalDate.of(2026, 12, 20));
        when(repository.findById(77L)).thenReturn(Optional.of(existing));
        when(repository.findByNumberAndCustomerDocument("123", "12345678000199"))
                .thenReturn(Optional.of(existing));

        service.updateManual(77L, "123", "Cliente atualizado", "12345678000199", "55",
                LocalDate.of(2026, 11, 3), List.of("1", "3"), List.of("1000.00", "1100.00"),
                List.of("2026-11-20", "2027-01-20"), List.of("250.00", ""), List.of("2026-11-10", ""));

        assertEquals("Cliente atualizado", existing.getCustomer());
        assertEquals(2, existing.getInstallments().size());
        assertEquals(1, existing.getInstallments().get(0).getInstallmentNumber());
        assertEquals(new BigDecimal("1000.00"), existing.getInstallments().get(0).getAmount());
        assertEquals(new BigDecimal("250.00"), existing.getInstallments().get(0).getPaidAmount());
        assertEquals(3, existing.getInstallments().get(1).getInstallmentNumber());
        assertEquals(BigDecimal.ZERO, existing.getInstallments().get(1).getPaidAmount());
        verify(repository).save(existing);
    }

    @Test
    void duplicateXmlImportIsIgnored() {
        Invoice existing = invoice("123", "12345678000199");
        when(repository.findByNumberAndCustomerDocument("123", "12345678000199"))
                .thenReturn(Optional.of(existing));
        String xml = """
                <nfeProc><NFe><infNFe><ide><nNF>123</nNF><mod>55</mod><dhEmi>2026-11-03T10:00:00-03:00</dhEmi></ide>
                <emit><CNPJ>99887766000155</CNPJ><xNome>Emitente</xNome></emit>
                <dest><CNPJ>12345678000199</CNPJ><xNome>Cliente</xNome></dest>
                <total><ICMSTot><vNF>1050.00</vNF></ICMSTot></total></infNFe></NFe></nfeProc>
                """;
        MockMultipartFile file = new MockMultipartFile("files", "nota.xml", "text/xml",
                xml.getBytes(StandardCharsets.UTF_8));

        String result = service.importXml(List.of(file));

        assertTrue(result.contains("0 nota(s) importada(s); 1 duplicada(s) ignorada(s)."));
        verify(repository, never()).save(any(Invoice.class));
    }

    private Invoice invoice(String number, String document) {
        LocalDate issueDate = LocalDate.of(2026, 11, 3);
        return new Invoice(number, "Cliente", document, "55", issueDate,
                YearMonth.from(issueDate).toString());
    }
}
