package com.example.service;

import com.example.domain.Order;
import com.example.domain.PaymentMethod;
import com.example.domain.PaymentStatus;
import com.example.repository.OrderRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ReportService {

    public enum ReportPeriod {
        HARI_INI("Hari Ini"),
        MINGGU_INI("Minggu Ini"),
        BULAN_INI("Bulan Ini"),
        CUSTOM("Custom");

        private final String displayName;

        ReportPeriod(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final OrderRepository orderRepository;
    private final SettingService settingService;

    public ReportService(OrderRepository orderRepository, SettingService settingService) {
        this.orderRepository = orderRepository;
        this.settingService = settingService;
    }

    public List<Order> getOrdersForReport(ReportPeriod period, LocalDate customStart, LocalDate customEnd,
                                          PaymentMethod methodFilter, PaymentStatus statusFilter) {
        LocalDateTime[] range = calculateDateRange(period, customStart, customEnd);
        List<Order> list = orderRepository.findOrdersInPeriod(range[0], range[1]);

        return list.stream()
                .filter(o -> methodFilter == null || o.getPaymentMethod() == methodFilter)
                .filter(o -> statusFilter == null || o.getPaymentStatus() == statusFilter)
                .toList();
    }

    public LocalDateTime[] calculateDateRange(ReportPeriod period, LocalDate customStart, LocalDate customEnd) {
        LocalDate today = LocalDate.now();
        LocalDateTime start;
        LocalDateTime end;

        switch (period) {
            case HARI_INI -> {
                start = today.atStartOfDay();
                end = today.plusDays(1).atStartOfDay().minusNanos(1);
            }
            case MINGGU_INI -> {
                LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                start = monday.atStartOfDay();
                end = sunday.plusDays(1).atStartOfDay().minusNanos(1);
            }
            case BULAN_INI -> {
                LocalDate firstDay = today.with(TemporalAdjusters.firstDayOfMonth());
                LocalDate lastDay = today.with(TemporalAdjusters.lastDayOfMonth());
                start = firstDay.atStartOfDay();
                end = lastDay.plusDays(1).atStartOfDay().minusNanos(1);
            }
            case CUSTOM -> {
                if (customStart == null) customStart = today;
                if (customEnd == null) customEnd = today;
                if (customEnd.isBefore(customStart)) {
                    throw new IllegalArgumentException("Tanggal akhir harus sama dengan atau setelah tanggal mulai.");
                }
                start = customStart.atStartOfDay();
                end = customEnd.plusDays(1).atStartOfDay().minusNanos(1);
            }
            default -> {
                start = today.atStartOfDay();
                end = today.plusDays(1).atStartOfDay().minusNanos(1);
            }
        }
        return new LocalDateTime[]{start, end};
    }

    public BigDecimal calculateTotalRevenue(List<Order> orders) {
        return orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateAverageOrderValue(List<Order> orders) {
        if (orders.isEmpty()) return BigDecimal.ZERO;
        BigDecimal total = calculateTotalRevenue(orders);
        return total.divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP);
    }

    public Map<String, BigDecimal> calculateDailyTrend(List<Order> orders, LocalDateTime start, LocalDateTime end) {
        Map<String, BigDecimal> trend = new LinkedHashMap<>();
        LocalDate cur = start.toLocalDate();
        LocalDate last = end.toLocalDate();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM");

        while (!cur.isAfter(last)) {
            trend.put(cur.format(dtf), BigDecimal.ZERO);
            cur = cur.plusDays(1);
        }

        for (Order o : orders) {
            String dayKey = o.getOrderDate().format(dtf);
            if (trend.containsKey(dayKey)) {
                trend.put(dayKey, trend.get(dayKey).add(o.getTotalAmount()));
            }
        }
        return trend;
    }

    public byte[] generateExcelReport(List<Order> orders, ReportPeriod period, LocalDate customStart, LocalDate customEnd) throws IOException {
        LocalDateTime[] range = calculateDateRange(period, customStart, customEnd);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        DateTimeFormatter dateOnly = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        String shopName = settingService.getSetting(SettingService.KEY_LAUNDRY_NAME, SettingService.DEFAULT_LAUNDRY_NAME);
        String address = settingService.getSetting(SettingService.KEY_LAUNDRY_ADDRESS, SettingService.DEFAULT_LAUNDRY_ADDRESS);
        String phone = settingService.getSetting(SettingService.KEY_LAUNDRY_PHONE, SettingService.DEFAULT_LAUNDRY_PHONE);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Laporan Penjualan");

            // Fonts & Styles
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(IndexedColors.VIOLET.getIndex());

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.VIOLET.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);

            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0"));
            currencyStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle boldCurrencyStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldCurrencyStyle.setFont(boldFont);
            boldCurrencyStyle.setDataFormat(format.getFormat("#,##0"));
            boldCurrencyStyle.setAlignment(HorizontalAlignment.RIGHT);
            boldCurrencyStyle.setBorderTop(BorderStyle.DOUBLE);

            // Title block
            int rowIdx = 0;
            Row r0 = sheet.createRow(rowIdx++);
            Cell c0 = r0.createCell(0);
            c0.setCellValue(shopName.toUpperCase() + " - LAPORAN PENJUALAN");
            c0.setCellStyle(titleStyle);

            Row r1 = sheet.createRow(rowIdx++);
            r1.createCell(0).setCellValue(address + " | Telp: " + phone);

            Row r2 = sheet.createRow(rowIdx++);
            r2.createCell(0).setCellValue("Periode: " + period.getDisplayName() +
                    " (" + range[0].format(dateOnly) + " s/d " + range[1].format(dateOnly) + ")");

            rowIdx++; // blank row

            // KPI Summary block
            BigDecimal totalRev = calculateTotalRevenue(orders);
            BigDecimal avgRev = calculateAverageOrderValue(orders);

            Row kpiHeader = sheet.createRow(rowIdx++);
            kpiHeader.createCell(0).setCellValue("Total Transaksi:");
            kpiHeader.createCell(1).setCellValue(orders.size() + " Pesanan");

            Row kpiRev = sheet.createRow(rowIdx++);
            kpiRev.createCell(0).setCellValue("Total Pendapatan:");
            Cell kpiRevCell = kpiRev.createCell(1);
            kpiRevCell.setCellValue(totalRev.doubleValue());
            kpiRevCell.setCellStyle(currencyStyle);

            Row kpiAvg = sheet.createRow(rowIdx++);
            kpiAvg.createCell(0).setCellValue("Rata-rata Transaksi:");
            Cell kpiAvgCell = kpiAvg.createCell(1);
            kpiAvgCell.setCellValue(avgRev.doubleValue());
            kpiAvgCell.setCellStyle(currencyStyle);

            rowIdx++; // blank row

            // Table Header
            String[] headers = {"No", "No. Invoice", "Tanggal Order", "Nama Pelanggan", "Status Cucian", "Metode Bayar", "Status Bayar", "Total (Rp)", "Terbayar (Rp)", "Sisa (Rp)"};
            Row headerRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int startDataRow = rowIdx + 1;
            int counter = 1;
            for (Order o : orders) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(counter++);
                row.createCell(1).setCellValue(o.getInvoiceNumber());
                row.createCell(2).setCellValue(o.getOrderDate().format(dtf));
                row.createCell(3).setCellValue(o.getCustomer() != null ? o.getCustomer().getName() : "-");
                row.createCell(4).setCellValue(o.getStatus().getDisplayName());
                row.createCell(5).setCellValue(o.getPaymentMethod().name());
                row.createCell(6).setCellValue(o.getPaymentStatus().getDisplayName());

                Cell totalCell = row.createCell(7);
                totalCell.setCellValue(o.getTotalAmount().doubleValue());
                totalCell.setCellStyle(currencyStyle);

                Cell paidCell = row.createCell(8);
                paidCell.setCellValue(o.getPaidAmount().doubleValue());
                paidCell.setCellStyle(currencyStyle);

                Cell remCell = row.createCell(9);
                remCell.setCellValue(o.getRemainingAmount().doubleValue());
                remCell.setCellStyle(currencyStyle);
            }

            // Total formula row
            if (!orders.isEmpty()) {
                Row totalRow = sheet.createRow(rowIdx++);
                Cell lbl = totalRow.createCell(6);
                lbl.setCellValue("TOTAL KESELURUHAN:");
                CellStyle boldStyle = workbook.createCellStyle();
                boldStyle.setFont(boldFont);
                boldStyle.setBorderTop(BorderStyle.DOUBLE);
                lbl.setCellStyle(boldStyle);

                Cell sumTotal = totalRow.createCell(7);
                sumTotal.setCellFormula("SUM(H" + startDataRow + ":H" + (rowIdx - 1) + ")");
                sumTotal.setCellStyle(boldCurrencyStyle);

                Cell sumPaid = totalRow.createCell(8);
                sumPaid.setCellFormula("SUM(I" + startDataRow + ":I" + (rowIdx - 1) + ")");
                sumPaid.setCellStyle(boldCurrencyStyle);

                Cell sumRem = totalRow.createCell(9);
                sumRem.setCellFormula("SUM(J" + startDataRow + ":J" + (rowIdx - 1) + ")");
                sumRem.setCellStyle(boldCurrencyStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
