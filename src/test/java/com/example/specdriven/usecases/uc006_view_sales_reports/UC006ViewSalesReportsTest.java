package com.example.specdriven.usecases.uc006_view_sales_reports;

import com.example.domain.*;
import com.example.service.CustomerService;
import com.example.service.OrderService;
import com.example.service.ReportService;
import com.example.service.ServicePackageService;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class UC006ViewSalesReportsTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ServicePackageService servicePackageService;

    private Order order1;
    private Order order2;

    @BeforeEach
    void setUp() {
        Customer c1 = customerService.createCustomer("Pelanggan Laporan 1", "081299900011", "");
        Customer c2 = customerService.createCustomer("Pelanggan Laporan 2", "081299900022", "");

        LaundryService s = servicePackageService.createService("Service Laporan", BigDecimal.valueOf(10000), ServiceUnit.KG, 24, true);

        order1 = orderService.createOrder(c1, null, List.of(new OrderItem(s, BigDecimal.valueOf(2), "")),
                LocalDateTime.now().plusDays(1), PaymentMethod.CASH, BigDecimal.valueOf(20000), "");

        order2 = orderService.createOrder(c2, null, List.of(new OrderItem(s, BigDecimal.valueOf(3), "")),
                LocalDateTime.now().plusDays(1), PaymentMethod.TRANSFER, BigDecimal.ZERO, "");
    }

    @Test
    void mainFlow_generateReportAndExcel() throws IOException {
        List<Order> orders = reportService.getOrdersForReport(ReportService.ReportPeriod.BULAN_INI, null, null, null, null);
        assertFalse(orders.isEmpty());

        BigDecimal total = reportService.calculateTotalRevenue(orders);
        assertTrue(total.compareTo(BigDecimal.valueOf(50000)) >= 0);

        BigDecimal avg = reportService.calculateAverageOrderValue(orders);
        assertTrue(avg.compareTo(BigDecimal.ZERO) > 0);

        byte[] excelBytes = reportService.generateExcelReport(orders, ReportService.ReportPeriod.BULAN_INI, null, null);
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);

        // Verify valid xlsx
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            assertNotNull(wb.getSheet("Laporan Penjualan"));
            assertTrue(wb.getSheet("Laporan Penjualan").getPhysicalNumberOfRows() > 5);
        }
    }

    @Test
    void af1_customDateRangeFilter() {
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now();

        List<Order> orders = reportService.getOrdersForReport(ReportService.ReportPeriod.CUSTOM, start, end, null, null);
        assertNotNull(orders);
        assertTrue(orders.stream().allMatch(o -> !o.getOrderDate().toLocalDate().isBefore(start) &&
                !o.getOrderDate().toLocalDate().isAfter(end)));
    }

    @Test
    void af2_filterByPaymentMethodAndStatus() {
        List<Order> cashOrders = reportService.getOrdersForReport(ReportService.ReportPeriod.BULAN_INI, null, null, PaymentMethod.CASH, null);
        assertTrue(cashOrders.stream().allMatch(o -> o.getPaymentMethod() == PaymentMethod.CASH));

        List<Order> unpaidOrders = reportService.getOrdersForReport(ReportService.ReportPeriod.BULAN_INI, null, null, null, PaymentStatus.BELUM_LUNAS);
        assertTrue(unpaidOrders.stream().allMatch(o -> o.getPaymentStatus() == PaymentStatus.BELUM_LUNAS));
    }

    @Test
    void af3_emptyPeriodHandling() throws IOException {
        LocalDate past1 = LocalDate.of(2020, 1, 1);
        LocalDate past2 = LocalDate.of(2020, 1, 31);

        List<Order> empty = reportService.getOrdersForReport(ReportService.ReportPeriod.CUSTOM, past1, past2, null, null);
        assertTrue(empty.isEmpty());

        assertEquals(BigDecimal.ZERO, reportService.calculateTotalRevenue(empty));
        assertEquals(BigDecimal.ZERO, reportService.calculateAverageOrderValue(empty));

        byte[] emptyExcel = reportService.generateExcelReport(empty, ReportService.ReportPeriod.CUSTOM, past1, past2);
        assertNotNull(emptyExcel);
    }

    @Test
    void br02_periodPresetsCalculateDynamicDates() {
        LocalDateTime[] hari = reportService.calculateDateRange(ReportService.ReportPeriod.HARI_INI, null, null);
        assertEquals(LocalDate.now(), hari[0].toLocalDate());

        LocalDateTime[] minggu = reportService.calculateDateRange(ReportService.ReportPeriod.MINGGU_INI, null, null);
        assertNotNull(minggu[0]);
        assertNotNull(minggu[1]);

        LocalDateTime[] bulan = reportService.calculateDateRange(ReportService.ReportPeriod.BULAN_INI, null, null);
        assertEquals(1, bulan[0].getDayOfMonth());
    }

    @Test
    void br03_customDateRangeEndDateMustBeGteStartDate() {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().minusDays(2);

        assertThrows(IllegalArgumentException.class, () -> {
            reportService.calculateDateRange(ReportService.ReportPeriod.CUSTOM, start, end);
        });
    }

    @Test
    void br04_revenueCalculationsBasedOnTotalOrderAmounts() {
        BigDecimal total = reportService.calculateTotalRevenue(List.of(order1, order2));
        assertEquals(BigDecimal.valueOf(50000), total);
    }

    @Test
    void br05_exportedExcelIsStandardXlsxFormat() throws IOException {
        byte[] bytes = reportService.generateExcelReport(List.of(order1, order2), ReportService.ReportPeriod.BULAN_INI, null, null);
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals("Laporan Penjualan", wb.getSheetName(0));
        }
    }
}
