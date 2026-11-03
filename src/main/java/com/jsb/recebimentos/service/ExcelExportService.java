package com.jsb.recebimentos.service;

import com.jsb.recebimentos.config.CompanyInfo;
import com.jsb.recebimentos.entity.Installment;
import com.jsb.recebimentos.entity.Invoice;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {
    private static final String[] HEADERS = {"NF", "Mês referência", "Cliente", "Documento", "Modelo", "Emissão", "Parcela", "Valor parcela", "Vencimento", "Pago", "Data pagamento", "Situação", "Dias até vencer", "Mês recebido"};
    private static final String NAVY = "0B2C45";
    private static final String BLUE = "123B5B";
    private static final String LIGHT = "F2F6F8";

    private final InvoiceService invoiceService;
    private final CompanyInfo company;

    public ExcelExportService(InvoiceService invoiceService, CompanyInfo company) {
        this.invoiceService = invoiceService;
        this.company = company;
    }

    public byte[] createWorkbook() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            List<Invoice> invoices = invoiceService.all();
            XSSFSheet report = workbook.createSheet("Relatório");
            createReportSheet(report, invoices, styles);
            XSSFSheet data = workbook.createSheet("Dados BI");
            createDataSheet(data, invoices, styles);
            workbook.setActiveSheet(0);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void createReportSheet(XSSFSheet sheet, List<Invoice> invoices, Styles styles) {
        sheet.setDisplayGridlines(false);
        sheet.setColumnWidth(0, 13 * 256);
        sheet.setColumnWidth(1, 16 * 256);
        sheet.setColumnWidth(2, 42 * 256);
        for (int i = 3; i < HEADERS.length; i++) sheet.setColumnWidth(i, i == 7 ? 18 * 256 : 17 * 256);
        for (int r = 0; r < 5; r++) {
            Row topRow = sheet.createRow(r);
            topRow.setHeightInPoints(r == 0 ? 34 : r == 4 ? 14 : 23);
        }
        setMergedHeader(sheet, 0, "ESC CONTÁBIL | CONTROLE DE RECEBÍVEIS", styles.title);
        setMergedHeader(sheet, 1, company.getName(), styles.company);
        setMergedHeader(sheet, 2, "CNPJ: " + company.getCnpj(), styles.meta);
        setMergedHeader(sheet, 3, "Relatório gerado em " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")), styles.meta);
        
        int headerRow = 6;
        writeHeader(sheet, headerRow, styles.header);
        int row = headerRow + 1;
        for (Invoice invoice : invoices) {
            for (Installment installment : invoice.getInstallments()) {
                writeDataRow(sheet, row, invoice, installment, styles, (row - headerRow) % 2 == 0);
                row++;
            }
        }
        finishTable(sheet, headerRow, row - 1);
        sheet.createRow(5).setHeightInPoints(12);
        sheet.setAutobreaks(true);
        sheet.getPrintSetup().setLandscape(true);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);
        sheet.setFitToPage(true);
        sheet.setMargin(Sheet.LeftMargin, 0.25);
        sheet.setMargin(Sheet.RightMargin, 0.25);
    }

    private void createDataSheet(XSSFSheet sheet, List<Invoice> invoices, Styles styles) {
        sheet.createFreezePane(0, 1);
        sheet.setDisplayGridlines(false);
        for (int i = 0; i < HEADERS.length; i++) sheet.setColumnWidth(i, i == 2 ? 42 * 256 : i == 7 ? 18 * 256 : 17 * 256);
        writeHeader(sheet, 0, styles.header);
        int row = 1;
        for (Invoice invoice : invoices) {
            for (Installment installment : invoice.getInstallments()) {
                writeDataRow(sheet, row, invoice, installment, styles, row % 2 == 1);
                row++;
            }
        }
        finishTable(sheet, 0, row - 1);
    }

    private void writeHeader(Sheet sheet, int rowNumber, CellStyle style) {
        Row row = sheet.createRow(rowNumber);
        row.setHeightInPoints(30);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(style);
        }
    }

    private void writeDataRow(Sheet sheet, int rowNumber, Invoice n, Installment i, Styles styles, boolean shaded) {
        Row row = sheet.createRow(rowNumber);
        row.setHeightInPoints(23);
        CellStyle text = shaded ? styles.shadedText : styles.text;
        CellStyle date = shaded ? styles.shadedDate : styles.date;
        CellStyle money = shaded ? styles.shadedMoney : styles.money;
        CellStyle number = shaded ? styles.shadedNumber : styles.number;
        setText(row, 0, n.getNumber(), text);
        setText(row, 1, n.getReferenceMonth(), text);
        setText(row, 2, n.getCustomer(), text);
        setText(row, 3, n.getCustomerDocument(), text);
        setText(row, 4, n.getModel(), text);
        setDate(row, 5, n.getIssueDate(), date);
        setNumber(row, 6, i.getInstallmentNumber(), number);
        setNumber(row, 7, i.getAmount().doubleValue(), money);
        setDate(row, 8, i.getDueDate(), date);
        setText(row, 9, i.getPaidAmount().signum() > 0 ? "Sim" : "Não", text);
        setDate(row, 10, i.getPaidDate(), date);
        setText(row, 11, i.getStatus(), text);
        setNumber(row, 12, i.getDaysToDue(), number);
        setText(row, 13, i.getReceivedMonth(), text);
    }

    private void finishTable(Sheet sheet, int headerRow, int lastRow) {
        sheet.createFreezePane(0, headerRow + 1);
        if (lastRow >= headerRow + 1) sheet.setAutoFilter(new CellRangeAddress(headerRow, lastRow, 0, HEADERS.length - 1));
        else sheet.setAutoFilter(new CellRangeAddress(headerRow, headerRow, 0, HEADERS.length - 1));
    }

    private void setMergedHeader(Sheet sheet, int rowNumber, String value, CellStyle style) {
        sheet.addMergedRegion(new CellRangeAddress(rowNumber, rowNumber, 0, HEADERS.length - 1));
        Row row = sheet.getRow(rowNumber);
        for (int column = 0; column < HEADERS.length; column++) {
            Cell cell = row.createCell(column);
            cell.setCellStyle(style);
        }
        row.getCell(0).setCellValue(value);
    }

    private void setText(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void setDate(Row row, int column, LocalDate value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) cell.setCellValue(java.sql.Date.valueOf(value));
        cell.setCellStyle(style);
    }

    private void setNumber(Row row, int column, double value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static class Styles {
        final CellStyle title, company, meta, header, text, shadedText, date, shadedDate, money, shadedMoney, number, shadedNumber;

        Styles(XSSFWorkbook workbook) {
            title = workbook.createCellStyle();
            title.setFont(font(workbook, 15, "FFFFFF", true));
            title.setFillForegroundColor(color(NAVY));
            title.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            title.setVerticalAlignment(VerticalAlignment.CENTER);
            title.setAlignment(HorizontalAlignment.LEFT);

            company = workbook.createCellStyle();
            company.setFont(font(workbook, 12, NAVY, true));
            company.setVerticalAlignment(VerticalAlignment.CENTER);
            company.setAlignment(HorizontalAlignment.LEFT);
            meta = workbook.createCellStyle();
            meta.setFont(font(workbook, 10, BLUE, false));
            meta.setVerticalAlignment(VerticalAlignment.CENTER);
            meta.setAlignment(HorizontalAlignment.LEFT);

            header = workbook.createCellStyle();
            header.setFont(font(workbook, 10, "FFFFFF", true));
            header.setFillForegroundColor(color(BLUE));
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            header.setAlignment(HorizontalAlignment.LEFT);
            header.setWrapText(true);
            border(header, color(BLUE));

            DataFormat format = workbook.createDataFormat();
            text = base(workbook, null, "172B3A", false);
            shadedText = base(workbook, color(LIGHT), "172B3A", false);
            date = base(workbook, null, "172B3A", false);
            shadedDate = base(workbook, color(LIGHT), "172B3A", false);
            date.setDataFormat(format.getFormat("dd/mm/yyyy"));
            shadedDate.setDataFormat(format.getFormat("dd/mm/yyyy"));
            money = base(workbook, null, "172B3A", true);
            shadedMoney = base(workbook, color(LIGHT), "172B3A", true);
            money.setDataFormat(format.getFormat("\"R$\" #,##0.00"));
            shadedMoney.setDataFormat(format.getFormat("\"R$\" #,##0.00"));
            number = base(workbook, null, "172B3A", false);
            shadedNumber = base(workbook, color(LIGHT), "172B3A", false);
            number.setDataFormat(format.getFormat("0"));
            shadedNumber.setDataFormat(format.getFormat("0"));
        }

        private static CellStyle base(XSSFWorkbook workbook, XSSFColor fill, String fontColor, boolean bold) {
            CellStyle style = workbook.createCellStyle();
            style.setFont(font(workbook, 10, fontColor, bold));
            if (fill != null) {
                ((XSSFCellStyle) style).setFillForegroundColor(fill);
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setAlignment(HorizontalAlignment.LEFT);
            border(style, color("DCE5EA"));
            return style;
        }

        private static XSSFFont font(XSSFWorkbook workbook, int size, String hex, boolean bold) {
            XSSFFont font = workbook.createFont();
            font.setFontName("Arial");
            font.setFontHeightInPoints((short) size);
            font.setBold(bold);
            font.setColor(color(hex));
            return font;
        }

        private static void border(CellStyle style, XSSFColor color) {
            XSSFCellStyle x = (XSSFCellStyle) style;
            x.setBorderBottom(BorderStyle.THIN);
            x.setBottomBorderColor(color);
        }

        private static XSSFColor color(String hex) {
            return new XSSFColor(new byte[]{(byte) Integer.parseInt(hex.substring(0, 2), 16), (byte) Integer.parseInt(hex.substring(2, 4), 16), (byte) Integer.parseInt(hex.substring(4, 6), 16)}, new DefaultIndexedColorMap());
        }
    }
}
