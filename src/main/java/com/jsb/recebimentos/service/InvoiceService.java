package com.jsb.recebimentos.service;

import com.jsb.recebimentos.entity.Installment;
import com.jsb.recebimentos.entity.Invoice;
import com.jsb.recebimentos.repository.InvoiceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class InvoiceService {
    private final InvoiceRepository repository;

    public InvoiceService(InvoiceRepository repository) {
        this.repository = repository;
    }

    public List<Invoice> all() {
        return repository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "issueDate"));
    }

    @Transactional
    public String importXml(List<MultipartFile> files) {
        int imported = 0, duplicates = 0;
        List<String> errors = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            try {
                Invoice invoice = parse(file.getBytes());
                if (repository.findByNumberAndCustomerDocument(invoice.getNumber(), invoice.getCustomerDocument()).isPresent()) {
                    duplicates++;
                    continue;
                }
                invoice.setOriginalXml(new String(file.getBytes(), StandardCharsets.UTF_8));
                repository.save(invoice);
                imported++;
            } catch (Exception e) {
                errors.add(file.getOriginalFilename() + ": " + e.getMessage());
            }
        }
        return imported + " nota(s) importada(s); " + duplicates + " duplicada(s) ignorada(s)." + (errors.isEmpty() ? "" : " Erros: " + String.join(" | ", errors));
    }

    @Transactional
    public void createManual(String number, String customer, String customerDocument, String model,
                             LocalDate issueDate, List<String> installmentNumbers, List<String> amounts,
                             List<String> dueDates, List<String> paidAmounts, List<String> paidDates) {
        saveManual(null, number, customer, customerDocument, model, issueDate, installmentNumbers, amounts, dueDates, paidAmounts, paidDates);
    }

    @Transactional
    public void updateManual(Long invoiceId, String number, String customer, String customerDocument, String model,
                             LocalDate issueDate, List<String> installmentNumbers, List<String> amounts,
                             List<String> dueDates, List<String> paidAmounts, List<String> paidDates) {
        saveManual(invoiceId, number, customer, customerDocument, model, issueDate, installmentNumbers, amounts, dueDates, paidAmounts, paidDates);
    }

    private void saveManual(Long invoiceId, String number, String customer, String customerDocument, String model,
                            LocalDate issueDate, List<String> installmentNumbers, List<String> amounts,
                            List<String> dueDates, List<String> paidAmounts, List<String> paidDates) {
        String cleanNumber = number == null ? "" : number.trim();
        String cleanCustomer = customer == null ? "" : customer.trim();
        String cleanDocument = customerDocument == null ? "" : customerDocument.replaceAll("\\D", "");
        String cleanModel = model == null ? "" : model.trim();
        if (cleanNumber.isBlank() || cleanCustomer.isBlank() || cleanDocument.isBlank() || cleanModel.isBlank() || issueDate == null) {
            throw new IllegalArgumentException("Preencha número da nota, cliente, CNPJ/CPF, modelo e data de emissão.");
        }
        Invoice invoice = invoiceId == null ? null : repository.findById(invoiceId).orElseThrow();
        Optional<Invoice> duplicate = repository.findByNumberAndCustomerDocument(cleanNumber, cleanDocument);
        if (duplicate.isPresent() && (invoice == null || !duplicate.get().getId().equals(invoiceId))) {
            throw new IllegalArgumentException("Já existe uma nota com esse número e CNPJ/CPF.");
        }
        if (installmentNumbers == null || amounts == null || dueDates == null
                || installmentNumbers.size() != amounts.size() || amounts.size() != dueDates.size()) {
            throw new IllegalArgumentException("Confira os dados das parcelas e tente novamente.");
        }
        if (paidAmounts == null || paidAmounts.isEmpty()) paidAmounts = new ArrayList<>(Collections.nCopies(amounts.size(), ""));
        if (paidDates == null || paidDates.isEmpty()) paidDates = new ArrayList<>(Collections.nCopies(amounts.size(), ""));
        if (paidAmounts.size() != amounts.size() || paidDates.size() != amounts.size()) {
            throw new IllegalArgumentException("Confira os campos opcionais de recebimento das parcelas.");
        }

        List<InstallmentDraft> drafts = new ArrayList<>();
        Set<Integer> seenNumbers = new HashSet<>();
        for (int index = 0; index < amounts.size(); index++) {
            String rawAmount = amounts.get(index) == null ? "" : amounts.get(index).trim();
            String rawDueDate = dueDates.get(index) == null ? "" : dueDates.get(index).trim();
            String rawInstallmentNumber = installmentNumbers.get(index) == null ? "" : installmentNumbers.get(index).trim();
            String rawPaidAmount = paidAmounts.get(index) == null ? "" : paidAmounts.get(index).trim();
            String rawPaidDate = paidDates.get(index) == null ? "" : paidDates.get(index).trim();
            if (rawAmount.isBlank() && rawDueDate.isBlank() && rawInstallmentNumber.isBlank() && rawPaidAmount.isBlank() && rawPaidDate.isBlank()) continue;
            try {
                int installmentNumber = Integer.parseInt(rawInstallmentNumber);
                BigDecimal amount = new BigDecimal(rawAmount);
                LocalDate dueDate = LocalDate.parse(rawDueDate);
                BigDecimal paidAmount = rawPaidAmount.isBlank() ? BigDecimal.ZERO : new BigDecimal(rawPaidAmount);
                LocalDate paidDate = paidAmount.signum() == 0 ? null : (rawPaidDate.isBlank() ? LocalDate.now() : LocalDate.parse(rawPaidDate));
                if (installmentNumber < 1 || !seenNumbers.add(installmentNumber)) {
                    throw new IllegalArgumentException("Os números das parcelas devem ser positivos e únicos.");
                }
                if (amount.signum() <= 0 || paidAmount.signum() < 0 || paidAmount.compareTo(amount) > 0) {
                    throw new IllegalArgumentException("O valor da parcela deve ser positivo e o valor recebido não pode ultrapassá-lo.");
                }
                drafts.add(new InstallmentDraft(installmentNumber, amount, dueDate, paidAmount, paidDate));
            } catch (NumberFormatException | java.time.format.DateTimeParseException ex) {
                throw new IllegalArgumentException("Confira número, valor e datas da parcela " + (index + 1) + ".");
            }
        }
        if (drafts.isEmpty()) throw new IllegalArgumentException("Adicione pelo menos uma parcela com valor e vencimento.");

        if (invoice == null) invoice = new Invoice(cleanNumber, cleanCustomer, cleanDocument, cleanModel, issueDate,
                YearMonth.from(issueDate).toString());
        else invoice.updateDetails(cleanNumber, cleanCustomer, cleanDocument, cleanModel, issueDate, YearMonth.from(issueDate).toString());

        Map<Integer, Installment> existing = new HashMap<>();
        for (Installment installment : invoice.getInstallments()) existing.put(installment.getInstallmentNumber(), installment);
        for (InstallmentDraft draft : drafts) {
            Installment installment = existing.remove(draft.number());
            if (installment == null) {
                invoice.addInstallment(draft.number(), draft.amount(), draft.dueDate());
                installment = invoice.getInstallments().get(invoice.getInstallments().size() - 1);
            }
            installment.updateDetails(draft.amount(), draft.dueDate(), draft.paidAmount(), draft.paidDate());
        }
        existing.values().forEach(invoice::removeInstallment);
        repository.save(invoice);
    }

    private record InstallmentDraft(int number, BigDecimal amount, LocalDate dueDate,
                                    BigDecimal paidAmount, LocalDate paidDate) { }

    private Invoice parse(byte[] bytes) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(false);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setFeature("http://xml.org/sax/features/external-general-entities", false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        Document d = f.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
        Element root = d.getDocumentElement();
        Element info = first(root, "infNFe");
        if (info == null) throw new IllegalArgumentException("XML não parece ser uma NF-e (infNFe ausente)");
        Element ide = first(info, "ide"), emit = first(info, "emit"), dest = first(info, "dest"), total = first(info, "ICMSTot");
        String number = val(ide, "nNF");
        if (number.isBlank()) throw new IllegalArgumentException("Número da NF não encontrado");
        String customer = val(emit, "xNome"), doc = val(emit, "CNPJ");
        if (doc.isBlank()) doc = val(emit, "CPF");
        // Se a empresa cadastrada for destinatária, use o destinatário; ainda assim preserve o emitente se não houver destinatário.
        String destName = val(dest, "xNome"), destDoc = val(dest, "CNPJ");
        if (destDoc.isBlank()) destDoc = val(dest, "CPF");
        if (!destName.isBlank()) {
            customer = destName;
            doc = destDoc;
        }
        String dh = val(ide, "dhEmi");
        if (dh.isBlank()) dh = val(ide, "dEmi");
        LocalDate issue = dh.isBlank() ? LocalDate.now() : LocalDate.parse(dh.substring(0, 10));
        Invoice inv = new Invoice(number, customer, doc, val(ide, "mod"), issue, YearMonth.from(issue).toString());
        Element cobr = first(info, "cobr");
        NodeList dups = cobr == null ? null : cobr.getElementsByTagName("dup");
        if (dups != null && dups.getLength() > 0) {
            for (int i = 0; i < dups.getLength(); i++) {
                Element dup = (Element) dups.item(i);
                String n = val(dup, "nDup");
                int seq = parseSeq(n, i + 1);
                String dv = val(dup, "dVenc"), v = val(dup, "vDup");
                if (!v.isBlank())
                    inv.addInstallment(seq, new BigDecimal(v), dv.isBlank() ? issue : LocalDate.parse(dv));
            }
        }
        if (inv.getInstallments().isEmpty()) {
            String value = val(total, "vNF");
            if (value.isBlank()) throw new IllegalArgumentException("Valor total da nota não encontrado");
            inv.addInstallment(1, new BigDecimal(value), issue.plusDays(30));
        }
        return inv;
    }

    private int parseSeq(String s, int fallback) {
        try {
            return Integer.parseInt(s.replaceAll("\\D", ""));
        } catch (Exception e) {
            return fallback;
        }
    }

    private Element first(Element parent, String tag) {
        if (parent == null) return null;
        NodeList list = parent.getElementsByTagName(tag);
        return list.getLength() == 0 ? null : (Element) list.item(0);
    }

    private String val(Element parent, String tag) {
        Element e = first(parent, tag);
        return e == null ? "" : e.getTextContent().trim();
    }

    @Transactional
    public void markPaid(Long id, BigDecimal amount, LocalDate date) {
        Installment i = repository.findAll().stream().flatMap(n -> n.getInstallments().stream()).filter(x -> x.getId().equals(id)).findFirst().orElseThrow();
        i.recordPayment(amount, date);
    }

    public Invoice find(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public String csv() {
        StringBuilder s = new StringBuilder("\uFEFFNF;Mês referência;Cliente;Documento;Modelo;Emissão;Parcela;Valor parcela;Vencimento;Pago;Data pagamento;Situação;Dias até vencer;Mês recebido\r\n");
        for (Invoice n : all())
            for (Installment i : n.getInstallments()) {
                s.append(row(n.getNumber())).append(';').append(row(n.getReferenceMonth())).append(';').append(row(n.getCustomer())).append(';').append(row(n.getCustomerDocument())).append(';').append(row(n.getModel())).append(';').append(date(n.getIssueDate())).append(';').append(i.getInstallmentNumber()).append(';').append(i.getAmount()).append(';').append(date(i.getDueDate())).append(';').append(i.getPaidAmount().signum() > 0 ? "Sim" : "Não").append(';').append(date(i.getPaidDate())).append(';').append(i.getStatus()).append(';').append(i.getDaysToDue()).append(';').append(i.getReceivedMonth()).append("\r\n");
            }
        return s.toString();
    }

    private String date(LocalDate d) {
        return d == null ? "" : d.toString();
    }

    private String row(String v) {
        return v == null ? "" : "\"" + v.replace("\"", "\"\"") + "\"";
    }
}
