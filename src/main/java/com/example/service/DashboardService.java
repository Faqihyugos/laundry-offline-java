package com.example.service;

import com.example.domain.Order;
import com.example.domain.OrderStatus;
import com.example.repository.OrderRepository;
import com.example.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public DashboardService(OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    public BigDecimal getTodayRevenue() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return paymentRepository.sumPaymentsInPeriod(start, end);
    }

    public long getTodayOrderCount() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return orderRepository.countByOrderDateBetween(start, end);
    }

    public long getCountByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    public List<Order> getLatest5Orders() {
        return orderRepository.findTop5ByOrderByOrderDateDesc();
    }

    public Map<String, BigDecimal> getWeeklyRevenueTrend() {
        Map<String, BigDecimal> trend = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter labelFormat = DateTimeFormatter.ofPattern("EEE (dd/MM)", java.util.Locale.forLanguageTag("id-ID"));

        // Last 7 days including today
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            BigDecimal dayRevenue = paymentRepository.sumPaymentsInPeriod(start, end);
            trend.put(date.format(labelFormat), dayRevenue);
        }
        return trend;
    }

    public static class DashboardSummary {
        private final BigDecimal todayRevenue;
        private final long todayOrders;
        private final long pendingCount;
        private final long prosesCount;
        private final long selesaiCount;
        private final long diambilCount;
        private final Map<String, BigDecimal> weeklyTrend;
        private final List<Order> recentOrders;

        public DashboardSummary(BigDecimal todayRevenue, long todayOrders, long pendingCount,
                                long prosesCount, long selesaiCount, long diambilCount,
                                Map<String, BigDecimal> weeklyTrend, List<Order> recentOrders) {
            this.todayRevenue = todayRevenue;
            this.todayOrders = todayOrders;
            this.pendingCount = pendingCount;
            this.prosesCount = prosesCount;
            this.selesaiCount = selesaiCount;
            this.diambilCount = diambilCount;
            this.weeklyTrend = weeklyTrend;
            this.recentOrders = recentOrders;
        }

        public BigDecimal getTodayRevenue() { return todayRevenue; }
        public long getTodayOrders() { return todayOrders; }
        public long getPendingCount() { return pendingCount; }
        public long getProsesCount() { return prosesCount; }
        public long getSelesaiCount() { return selesaiCount; }
        public long getDiambilCount() { return diambilCount; }
        public Map<String, BigDecimal> getWeeklyTrend() { return weeklyTrend; }
        public List<Order> getRecentOrders() { return recentOrders; }
    }

    public DashboardSummary getSummary() {
        return new DashboardSummary(
                getTodayRevenue(),
                getTodayOrderCount(),
                getCountByStatus(OrderStatus.PENDING),
                getCountByStatus(OrderStatus.PROSES),
                getCountByStatus(OrderStatus.SELESAI),
                getCountByStatus(OrderStatus.DIAMBIL),
                getWeeklyRevenueTrend(),
                getLatest5Orders()
        );
    }
}
