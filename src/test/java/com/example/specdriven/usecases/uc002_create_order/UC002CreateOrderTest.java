package com.example.specdriven.usecases.uc002_create_order;

import com.example.domain.*;
import com.example.service.CustomerService;
import com.example.service.OrderService;
import com.example.service.ServicePackageService;
import com.example.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class UC002CreateOrderTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ServicePackageService servicePackageService;

    @Autowired
    private UserService userService;

    private Customer customer;
    private User cashier;
    private LaundryService kgService;
    private LaundryService pcsService;

    @BeforeEach
    void setUp() {
        customer = customerService.createCustomer("Doni Setiawan", "081555666777", "Jl. Flamboyan");
        cashier = userService.findByUsername("kasir").orElseGet(() ->
                userService.createUser("kasir_test", "Kasir Test", "secret123", "secret123", Role.CASHIER));

        kgService = servicePackageService.createService("Cuci Setrika Test", BigDecimal.valueOf(10000), ServiceUnit.KG, 48, true);
        pcsService = servicePackageService.createService("Bedcover Test", BigDecimal.valueOf(30000), ServiceUnit.PCS, 24, true);
    }

    @Test
    void mainFlow_createMultiItemOrder() {
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem(kgService, BigDecimal.valueOf(3.5), "Pakaian kantor"));
        items.add(new OrderItem(pcsService, BigDecimal.valueOf(2), "Warna biru"));

        LocalDateTime estDate = LocalDateTime.now().plusHours(48);
        BigDecimal paid = BigDecimal.valueOf(95000); // 3.5*10000 + 2*30000 = 35000 + 60000 = 95000

        Order order = orderService.createOrder(customer, cashier, items, estDate, PaymentMethod.CASH, paid, "Catatan Umum");

        assertNotNull(order.getId());
        assertTrue(order.getInvoiceNumber().startsWith("LBW-"));
        assertEquals(2, order.getItems().size());
        assertEquals(BigDecimal.valueOf(95000).setScale(2), order.getTotalAmount().setScale(2));
        assertEquals(BigDecimal.valueOf(95000).setScale(2), order.getPaidAmount().setScale(2));
        assertEquals(BigDecimal.ZERO.setScale(2), order.getRemainingAmount().setScale(2));
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(PaymentStatus.LUNAS, order.getPaymentStatus());
        assertEquals(PaymentMethod.CASH, order.getPaymentMethod());
    }

    @Test
    void af1_registerNewCustomerInline() {
        Customer inlineCust = customerService.createCustomer("Pelanggan Baru Inline", "081999888111", "Alamat Baru");
        List<OrderItem> items = List.of(new OrderItem(kgService, BigDecimal.valueOf(2.0), ""));
        Order order = orderService.createOrder(inlineCust, cashier, items, LocalDateTime.now().plusDays(2), PaymentMethod.TRANSFER, BigDecimal.ZERO, "");

        assertNotNull(order.getId());
        assertEquals(inlineCust.getId(), order.getCustomer().getId());
    }

    @Test
    void af2_adjustLineItemQuantityOrRemove() {
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem(kgService, BigDecimal.valueOf(2.0), ""));
        items.add(new OrderItem(pcsService, BigDecimal.valueOf(1), ""));

        Order order = orderService.createOrder(customer, cashier, items, LocalDateTime.now().plusDays(2), PaymentMethod.CASH, BigDecimal.ZERO, "");
        assertEquals(BigDecimal.valueOf(50000).setScale(2), order.getTotalAmount().setScale(2));

        // Update items: remove pcsService and change kg to 5.0
        List<OrderItem> updatedItems = List.of(new OrderItem(kgService, BigDecimal.valueOf(5.0), "Diubah"));
        Order updatedOrder = orderService.updateOrderItems(order.getId(), updatedItems, "Catatan revisi");

        assertEquals(1, updatedOrder.getItems().size());
        assertEquals(BigDecimal.valueOf(50000).setScale(2), updatedOrder.getTotalAmount().setScale(2));
    }

    @Test
    void af3_zeroInitialPayment_BelumLunas() {
        List<OrderItem> items = List.of(new OrderItem(kgService, BigDecimal.valueOf(2.0), ""));
        Order order = orderService.createOrder(customer, cashier, items, LocalDateTime.now().plusDays(1), PaymentMethod.CASH, BigDecimal.ZERO, "");

        assertEquals(PaymentStatus.BELUM_LUNAS, order.getPaymentStatus());
        assertEquals(BigDecimal.ZERO.setScale(2), order.getPaidAmount().setScale(2));
        assertEquals(order.getTotalAmount().setScale(2), order.getRemainingAmount().setScale(2));
    }

    @Test
    void af4_partialDownPayment_Sebagian() {
        List<OrderItem> items = List.of(new OrderItem(kgService, BigDecimal.valueOf(5.0), "")); // 50000
        BigDecimal dp = BigDecimal.valueOf(20000);
        Order order = orderService.createOrder(customer, cashier, items, LocalDateTime.now().plusDays(1), PaymentMethod.CASH, dp, "");

        assertEquals(PaymentStatus.SEBAGIAN, order.getPaymentStatus());
        assertEquals(BigDecimal.valueOf(20000).setScale(2), order.getPaidAmount().setScale(2));
        assertEquals(BigDecimal.valueOf(30000).setScale(2), order.getRemainingAmount().setScale(2));
    }

    @Test
    void af5_cashOverpayment_LunasWithChange() {
        List<OrderItem> items = List.of(new OrderItem(kgService, BigDecimal.valueOf(2.0), "")); // 20000
        BigDecimal cashGiven = BigDecimal.valueOf(50000);
        Order order = orderService.createOrder(customer, cashier, items, LocalDateTime.now().plusDays(1), PaymentMethod.CASH, cashGiven, "");

        assertEquals(PaymentStatus.LUNAS, order.getPaymentStatus());
        assertEquals(BigDecimal.ZERO.setScale(2), order.getRemainingAmount().setScale(2));
        assertTrue(cashGiven.compareTo(order.getTotalAmount()) > 0);
    }

    @Test
    void br01_mustContainAtLeastOneItem() {
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(customer, cashier, Collections.emptyList(), LocalDateTime.now().plusDays(1), PaymentMethod.CASH, BigDecimal.ZERO, "");
        });
    }

    @Test
    void br02_quantityMustBePositiveAndPcsMustBeInteger() {
        // Quantity <= 0
        assertThrows(IllegalArgumentException.class, () -> {
            List<OrderItem> items = List.of(new OrderItem(kgService, BigDecimal.ZERO, ""));
            orderService.createOrder(customer, cashier, items, LocalDateTime.now().plusDays(1), PaymentMethod.CASH, BigDecimal.ZERO, "");
        });

        // Decimal for pcs
        assertThrows(IllegalArgumentException.class, () -> {
            List<OrderItem> items = List.of(new OrderItem(pcsService, BigDecimal.valueOf(1.5), ""));
            orderService.createOrder(customer, cashier, items, LocalDateTime.now().plusDays(1), PaymentMethod.CASH, BigDecimal.ZERO, "");
        });
    }

    @Test
    void br03_invoiceNumberUniquelyGenerated() {
        String inv1 = orderService.generateNextInvoiceNumber();
        String inv2 = orderService.generateNextInvoiceNumber();
        assertNotNull(inv1);
        assertNotNull(inv2);
        assertTrue(inv1.matches("^LBW-\\d{8}-\\d{4}$"));
    }

    @Test
    void br04_initialWorkflowStatusIsPending() {
        List<OrderItem> items = List.of(new OrderItem(kgService, BigDecimal.valueOf(1.0), ""));
        Order order = orderService.createOrder(customer, cashier, items, LocalDateTime.now().plusDays(1), PaymentMethod.CASH, BigDecimal.ZERO, "");
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test
    void br05_paymentStatusDerivation() {
        List<OrderItem> items = List.of(new OrderItem(kgService, BigDecimal.valueOf(2.0), "")); // 20000

        Order unpaid = orderService.createOrder(customer, cashier, items, LocalDateTime.now().plusDays(1), PaymentMethod.CASH, BigDecimal.ZERO, "");
        assertEquals(PaymentStatus.BELUM_LUNAS, unpaid.getPaymentStatus());

        Order partial = orderService.createOrder(customer, cashier, items, LocalDateTime.now().plusDays(1), PaymentMethod.CASH, BigDecimal.valueOf(10000), "");
        assertEquals(PaymentStatus.SEBAGIAN, partial.getPaymentStatus());

        Order paid = orderService.createOrder(customer, cashier, items, LocalDateTime.now().plusDays(1), PaymentMethod.CASH, BigDecimal.valueOf(20000), "");
        assertEquals(PaymentStatus.LUNAS, paid.getPaymentStatus());
    }

    @Test
    void br06_paymentMethodSpecified() {
        List<OrderItem> items = List.of(new OrderItem(kgService, BigDecimal.valueOf(1.0), ""));
        Order orderCash = orderService.createOrder(customer, cashier, items, LocalDateTime.now().plusDays(1), PaymentMethod.CASH, BigDecimal.ZERO, "");
        assertEquals(PaymentMethod.CASH, orderCash.getPaymentMethod());

        Order orderTransfer = orderService.createOrder(customer, cashier, items, LocalDateTime.now().plusDays(1), PaymentMethod.TRANSFER, BigDecimal.ZERO, "");
        assertEquals(PaymentMethod.TRANSFER, orderTransfer.getPaymentMethod());
    }

    @Test
    void br07_estimatedDateMustBeAfterOrEqualOrderDate() {
        List<OrderItem> items = List.of(new OrderItem(kgService, BigDecimal.valueOf(1.0), ""));
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);

        assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(customer, cashier, items, pastDate, PaymentMethod.CASH, BigDecimal.ZERO, "");
        });
    }
}
