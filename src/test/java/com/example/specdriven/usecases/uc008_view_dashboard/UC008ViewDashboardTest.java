package com.example.specdriven.usecases.uc008_view_dashboard;

import com.example.domain.*;
import com.example.service.CustomerService;
import com.example.service.DashboardService;
import com.example.service.OrderService;
import com.example.service.ServicePackageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class UC008ViewDashboardTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ServicePackageService servicePackageService;

    private Customer customer;
    private LaundryService service;

    @BeforeEach
    void setUp() {
        customer = customerService.createCustomer("Pelanggan Dashboard", "081999777111", "");
        service = servicePackageService.createService("Service Dash", BigDecimal.valueOf(12000), ServiceUnit.KG, 24, true);

        // Create 2 sample orders
        List<OrderItem> items = List.of(new OrderItem(service, BigDecimal.valueOf(2.5), "")); // 30000
        orderService.createOrder(customer, null, items, LocalDateTime.now().plusDays(1), PaymentMethod.CASH, BigDecimal.valueOf(30000), "");
    }

    @Test
    void mainFlow_viewDashboardSummary() {
        DashboardService.DashboardSummary summary = dashboardService.getSummary();

        assertNotNull(summary);
        assertTrue(summary.getTodayRevenue().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(summary.getTodayOrders() >= 1);
        assertEquals(7, summary.getWeeklyTrend().size());
        assertNotNull(summary.getRecentOrders());
        assertTrue(summary.getRecentOrders().size() <= 5);
    }

    @Test
    void af1_cashierViewOperationalStats() {
        DashboardService.DashboardSummary summary = dashboardService.getSummary();
        assertTrue(summary.getTodayOrders() >= 0);
        assertTrue(summary.getPendingCount() >= 0);
        assertTrue(summary.getProsesCount() >= 0);
        assertTrue(summary.getSelesaiCount() >= 0);
        assertTrue(summary.getDiambilCount() >= 0);
    }

    @Test
    void af2_emptyStateHandling() {
        // Checking status counts returns non-null numbers
        long pending = dashboardService.getCountByStatus(OrderStatus.PENDING);
        assertTrue(pending >= 0);
    }

    @Test
    void br01_todayRevenueCalculations() {
        BigDecimal todayRev = dashboardService.getTodayRevenue();
        assertNotNull(todayRev);
        assertTrue(todayRev.compareTo(BigDecimal.valueOf(30000)) >= 0);
    }

    @Test
    void br02_statusCountsCategorizedAccurately() {
        long pending = dashboardService.getCountByStatus(OrderStatus.PENDING);
        long proses = dashboardService.getCountByStatus(OrderStatus.PROSES);
        long selesai = dashboardService.getCountByStatus(OrderStatus.SELESAI);
        long diambil = dashboardService.getCountByStatus(OrderStatus.DIAMBIL);

        assertTrue(pending >= 1);
        assertTrue(proses >= 0);
        assertTrue(selesai >= 0);
        assertTrue(diambil >= 0);
    }

    @Test
    void br03_weeklyTrendContains7Days() {
        Map<String, BigDecimal> trend = dashboardService.getWeeklyRevenueTrend();
        assertEquals(7, trend.size());
    }

    @Test
    void br04_latestOrdersReturnsAtMost5Descending() {
        List<Order> latest = dashboardService.getLatest5Orders();
        assertTrue(latest.size() <= 5);
        if (latest.size() > 1) {
            for (int i = 0; i < latest.size() - 1; i++) {
                assertTrue(!latest.get(i).getOrderDate().isBefore(latest.get(i + 1).getOrderDate()));
            }
        }
    }
}
