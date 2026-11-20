package com.example.service;

import com.example.domain.*;
import com.example.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final SettingService settingService;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        PaymentRepository paymentRepository,
                        SettingService settingService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.settingService = settingService;
    }

    public List<Order> findAll() {
        return orderRepository.findAllByOrderByOrderDateDesc();
    }

    public List<Order> findByStatus(OrderStatus status) {
        return orderRepository.findByStatusOrderByOrderDateDesc(status);
    }

    public List<Order> findByCustomer(Customer customer) {
        return orderRepository.findByCustomerOrderByOrderDateDesc(customer);
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public Optional<Order> findByInvoiceNumber(String invoiceNumber) {
        return orderRepository.findByInvoiceNumber(invoiceNumber);
    }

    public List<Order> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }
        return orderRepository.searchOrders(query.trim());
    }

    public List<Order> searchByStatus(OrderStatus status, String query) {
        if (query == null || query.trim().isEmpty()) {
            return findByStatus(status);
        }
        return orderRepository.searchOrdersByStatus(status, query.trim());
    }

    public synchronized String generateNextInvoiceNumber() {
        String prefix = settingService.getSetting(SettingService.KEY_INVOICE_PREFIX, SettingService.DEFAULT_INVOICE_PREFIX);
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        long countToday = orderRepository.countByOrderDateBetween(startOfDay, endOfDay) + 1;

        String formattedSeq = String.format("%04d", countToday);
        String candidate = prefix + "-" + dateStr + "-" + formattedSeq;

        // Ensure uniqueness if race condition occurred
        int seq = (int) countToday;
        while (orderRepository.findByInvoiceNumber(candidate).isPresent()) {
            seq++;
            candidate = prefix + "-" + dateStr + "-" + String.format("%04d", seq);
        }
        return candidate;
    }

    public Order createOrder(Customer customer,
                            User cashier,
                            List<OrderItem> items,
                            LocalDateTime estimatedCompletionDate,
                            PaymentMethod paymentMethod,
                            BigDecimal paidAmount,
                            String notes) {
        return createOrder(customer, cashier, items, estimatedCompletionDate, paymentMethod, paidAmount, notes, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public Order createOrder(Customer customer,
                            User cashier,
                            List<OrderItem> items,
                            LocalDateTime estimatedCompletionDate,
                            PaymentMethod paymentMethod,
                            BigDecimal paidAmount,
                            String notes,
                            BigDecimal discountAmount,
                            BigDecimal taxAmount) {
        if (customer == null) {
            throw new IllegalArgumentException("Pelanggan wajib dipilih.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Pesanan harus memiliki minimal 1 layanan.");
        }
        for (OrderItem item : items) {
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Jumlah kuantitas layanan harus lebih besar dari 0.");
            }
            if (item.getUnit() == ServiceUnit.PCS && item.getQuantity().stripTrailingZeros().scale() > 0) {
                throw new IllegalArgumentException("Layanan dengan satuan pcs harus berupa bilangan bulat.");
            }
        }

        LocalDateTime orderDate = LocalDateTime.now();
        if (estimatedCompletionDate != null && estimatedCompletionDate.isBefore(orderDate)) {
            throw new IllegalArgumentException("Estimasi tanggal selesai harus sama dengan atau setelah tanggal pemesanan.");
        }

        // Calculate subtotal and final total
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : items) {
            BigDecimal sub = item.getUnitPrice().multiply(item.getQuantity());
            item.setSubtotal(sub);
            subtotal = subtotal.add(sub);
        }

        BigDecimal discount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        BigDecimal tax = taxAmount != null ? taxAmount : BigDecimal.ZERO;
        BigDecimal total = subtotal.subtract(discount).add(tax);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) < 0) {
            paidAmount = BigDecimal.ZERO;
        }

        Order order = new Order();
        order.setInvoiceNumber(generateNextInvoiceNumber());
        order.setCustomer(customer);
        order.setCashier(cashier);
        order.setOrderDate(orderDate);
        order.setEstimatedCompletionDate(estimatedCompletionDate != null ? estimatedCompletionDate : calculateDefaultEstimatedDate(items));
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(paymentMethod != null ? paymentMethod : PaymentMethod.CASH);
        order.setTotalAmount(total);
        order.setNotes(notes);
        order.setStatusUpdatedAt(orderDate);
        order.setStatusUpdatedBy(cashier != null ? cashier.getFullName() : "System");

        derivePaymentStatus(order, paidAmount);

        for (OrderItem item : items) {
            order.addItem(item);
        }

        Order saved = orderRepository.save(order);

        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            Payment payment = new Payment(paidAmount, order.getPaymentMethod(), "Pembayaran awal saat pemesanan");
            saved.addPayment(payment);
            paymentRepository.save(payment);
        }

        return saved;
    }

    public Order updateOrderItems(Long orderId, List<OrderItem> newItems, String notes) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Pesanan yang sudah diproses tidak dapat diubah rincian layanannya.");
        }
        if (newItems == null || newItems.isEmpty()) {
            throw new IllegalArgumentException("Pesanan harus memiliki minimal 1 layanan.");
        }

        order.getItems().clear();
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : newItems) {
            if (item.getQuantity() == null || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Jumlah kuantitas layanan harus lebih besar dari 0.");
            }
            BigDecimal sub = item.getUnitPrice().multiply(item.getQuantity());
            item.setSubtotal(sub);
            total = total.add(sub);
            order.addItem(item);
        }

        order.setTotalAmount(total);
        order.setNotes(notes);
        derivePaymentStatus(order, order.getPaidAmount());
        return orderRepository.save(order);
    }

    public Order advanceStatus(Long orderId, String updatingUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan"));

        OrderStatus current = order.getStatus();
        if (current == OrderStatus.DIAMBIL) {
            throw new IllegalStateException("Pesanan sudah selesai diambil.");
        }

        OrderStatus next = current.next();

        if (next == OrderStatus.DIAMBIL && order.getPaymentStatus() != PaymentStatus.LUNAS) {
            throw new IllegalStateException("Pesanan belum lunas. Selesaikan sisa pembayaran sebelum menandai Diambil.");
        }

        order.setStatus(next);
        order.setStatusUpdatedAt(LocalDateTime.now());
        order.setStatusUpdatedBy(updatingUser != null ? updatingUser : "Staff");
        return orderRepository.save(order);
    }

    public Order settlePaymentAndAdvanceToDiambil(Long orderId, BigDecimal amount, PaymentMethod method, String updatingUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order tidak ditemukan"));

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Nominal pelunasan harus lebih besar dari 0.");
        }

        BigDecimal remaining = order.getRemainingAmount();
        if (amount.compareTo(remaining) < 0) {
            throw new IllegalArgumentException("Nominal pelunasan kurang dari sisa tagihan (Rp " + remaining + ").");
        }

        // Record payment
        Payment payment = new Payment(amount, method != null ? method : PaymentMethod.CASH, "Pelunasan saat pengambilan cucian");
        order.addPayment(payment);
        paymentRepository.save(payment);

        BigDecimal newPaid = order.getPaidAmount().add(amount);
        order.setPaidAmount(newPaid);
        order.setRemainingAmount(BigDecimal.ZERO);
        order.setPaymentStatus(PaymentStatus.LUNAS);

        order.setStatus(OrderStatus.DIAMBIL);
        order.setStatusUpdatedAt(LocalDateTime.now());
        order.setStatusUpdatedBy(updatingUser != null ? updatingUser : "Staff");

        return orderRepository.save(order);
    }

    private void derivePaymentStatus(Order order, BigDecimal paidAmount) {
        BigDecimal total = order.getTotalAmount();
        if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            order.setPaidAmount(BigDecimal.ZERO);
            order.setRemainingAmount(total);
            order.setPaymentStatus(PaymentStatus.BELUM_LUNAS);
        } else if (paidAmount.compareTo(total) >= 0) {
            order.setPaidAmount(paidAmount);
            order.setRemainingAmount(BigDecimal.ZERO);
            order.setPaymentStatus(PaymentStatus.LUNAS);
        } else {
            order.setPaidAmount(paidAmount);
            order.setRemainingAmount(total.subtract(paidAmount));
            order.setPaymentStatus(PaymentStatus.SEBAGIAN);
        }
    }

    public LocalDateTime calculateDefaultEstimatedDate(List<OrderItem> items) {
        int maxDurationHours = 24; // default minimum turnaround 24 hours
        if (items != null) {
            for (OrderItem item : items) {
                if (item.getService() != null && item.getService().getEstimatedDurationHours() > maxDurationHours) {
                    maxDurationHours = item.getService().getEstimatedDurationHours();
                }
            }
        }
        return LocalDateTime.now().plusHours(maxDurationHours);
    }
}
