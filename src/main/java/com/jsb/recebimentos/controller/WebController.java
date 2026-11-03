package com.jsb.recebimentos.controller;

import com.jsb.recebimentos.entity.Installment;
import com.jsb.recebimentos.entity.Invoice;
import com.jsb.recebimentos.config.CompanyInfo;
import com.jsb.recebimentos.service.ExcelExportService;
import com.jsb.recebimentos.service.InvoiceService;

import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class WebController {
    private final InvoiceService service;
    private final ExcelExportService excelExportService;
    private final CompanyInfo company;

    public WebController(InvoiceService service, ExcelExportService excelExportService, CompanyInfo company) {
        this.service = service;
        this.excelExportService = excelExportService;
        this.company = company;
    }

    @GetMapping("/")
    public String home(Model model, @RequestParam(required = false) String q, @RequestParam(required = false) String status,
                       @RequestParam(required = false) String view) {
        List<Invoice> invoices = service.all();
        model.addAttribute("invoices", invoices);
        model.addAttribute("companyName", company.getName());
        model.addAttribute("companyCnpj", company.getCnpj());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", status == null ? "" : status);
        model.addAttribute("activeView", "notas".equals(view) ? "notas" : "overview");
        long count = invoices.stream().flatMap(n -> n.getInstallments().stream()).count();
        long paid = invoices.stream().flatMap(n -> n.getInstallments().stream()).filter(i -> i.getStatus().equals("PAGO")).count();
        model.addAttribute("invoiceCount", invoices.size());
        model.addAttribute("installmentCount", count);
        model.addAttribute("paidCount", paid);
        model.addAttribute("openAmount", invoices.stream().flatMap(n -> n.getInstallments().stream()).map(Installment::getOpenAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        return "index";
    }

    @PostMapping("/import")
    public String upload(@RequestParam("files") List<MultipartFile> files, org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        flash.addFlashAttribute("message", service.importXml(files));
        return "redirect:/";
    }

    @PostMapping("/invoices/manual")
    public String createManual(@RequestParam String number,
                               @RequestParam String customer,
                               @RequestParam String customerDocument,
                               @RequestParam String model,
                               @RequestParam LocalDate issueDate,
                               @RequestParam List<String> installmentNumbers,
                               @RequestParam List<String> amounts,
                               @RequestParam List<String> dueDates,
                               @RequestParam(required = false) List<String> paidAmounts,
                               @RequestParam(required = false) List<String> paidDates,
                               org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        try {
            service.createManual(number, customer, customerDocument, model, issueDate,
                    installmentNumbers, amounts, dueDates, paidAmounts, paidDates);
            flash.addFlashAttribute("message", "Nota cadastrada manualmente com sucesso.");
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("message", exception.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/invoices/{id}/edit")
    public String editInvoice(@PathVariable Long id, Model model) {
        model.addAttribute("invoice", service.find(id));
        return "edit-invoice";
    }

    @PostMapping("/invoices/{id}/edit")
    public String updateInvoice(@PathVariable Long id,
                                @RequestParam String number,
                                @RequestParam String customer,
                                @RequestParam String customerDocument,
                                @RequestParam String model,
                                @RequestParam LocalDate issueDate,
                                @RequestParam List<String> installmentNumbers,
                                @RequestParam List<String> amounts,
                                @RequestParam List<String> dueDates,
                                @RequestParam(required = false) List<String> paidAmounts,
                                @RequestParam(required = false) List<String> paidDates,
                                org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        try {
            service.updateManual(id, number, customer, customerDocument, model, issueDate,
                    installmentNumbers, amounts, dueDates, paidAmounts, paidDates);
            flash.addFlashAttribute("message", "Nota e parcelas atualizadas com sucesso.");
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("message", exception.getMessage());
        }
        return "redirect:/?view=notas";
    }

    @PostMapping("/installments/{id}/paid")
    public String paid(@PathVariable Long id, @RequestParam(required = false) BigDecimal amount, @RequestParam(required = false) LocalDate paidDate, org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        try {
            service.markPaid(id, amount, paidDate);
            flash.addFlashAttribute("message", "Recebimento registrado.");
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("message", exception.getMessage());
        }
        return "redirect:/?view=notas";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> export() {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=controle-recebimentos.csv").contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8)).body(service.csv().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> exportExcel() throws java.io.IOException {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=esc-contabil-recebiveis.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelExportService.createWorkbook());
    }
}
