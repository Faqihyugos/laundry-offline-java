package com.example.specdriven.usecases.uc003_update_order_status;

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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class UC003UpdateOrderStatusTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ServicePackageService servicePackageService;

    @Autowired
    private UserService userService;

    private Order paidOrder;
    private Order unpaidOrder;
    private LaundryService service;

    @BeforeEach
    void setUp() {
        Customer customer = customerService.createCustomer("Rina Melati", "081333444555", "Jl. Kenanga 10");
        service = servicePackageService.createService("Cuci Kering UC003", BigDecimal.valueOf(8000), ServiceUnit.KG, 24, true);

        List<OrderItem> items1 = List.of(new OrderItem(service, BigDecimal.valueOf(3.0), ""));
        paidOrder = orderService.createOrder(customer, null, items1, LocalDateTime.now().plusDays(1), PaymentMethod.CASH, BigDecimal.valueOf(24000), "");

        List<OrderItem> items2 = List.of(new OrderItem(service, BigDecimal.valueOf(2.0), ""));
        unpaidOrder = orderService.createOrder(customer, null, items2, LocalDateTime.now().plusDays(1), PaymentMethod.CASH, BigDecimal.ZERO, "");
    }

    @Test
    void mainFlow_fullStatusProgression() {
        // Initial: PENDING
        assertEquals(OrderStatus.PENDING, paidOrder.getStatus());

        // Step 1: PENDING -> PROSES
        Order proses = orderService.advanceStatus(paidOrder.getId(), "Staff Cucian");
        assertEquals(OrderStatus.PROSES, proses.getStatus());
        assertEquals("Staff Cucian", proses.getStatusUpdatedBy());
        assertNotNull(proses.getStatusUpdatedAt());

        // Step 2: PROSES -> SELESAI
        Order selesai = orderService.advanceStatus(proses.getId(), "Staff Finishing");
        assertEquals(OrderStatus.SELESAI, selesai.getStatus());
        assertEquals("Staff Finishing", selesai.getStatusUpdatedBy());

        // Step 3: SELESAI -> DIAMBIL (Since paid in full)
        Order diambil = orderService.advanceStatus(selesai.getId(), "Kasir Kasir");
        assertEquals(OrderStatus.DIAMBIL, diambil.getStatus());
        assertEquals("Kasir Kasir", diambil.getStatusUpdatedBy());
    }

    @Test
    void af1_filterAndSearchOrders() {
        List<Order> pendingList = orderService.findByStatus(OrderStatus.PENDING);
        assertFalse(pendingList.isEmpty());
        assertTrue(pendingList.stream().allMatch(o -> o.getStatus() == OrderStatus.PENDING));

        List<Order> searchResult = orderService.search("Rina");
        assertFalse(searchResult.isEmpty());
        assertTrue(searchResult.stream().anyMatch(o -> o.getCustomer().getName().contains("Rina")));
    }

    @Test
    void af2_settleRemainingPaymentOnPickup() {
        // Advance unpaid order to PROSES then SELESAI
        orderService.advanceStatus(unpaidOrder.getId(), "Staff");
        orderService.advanceStatus(unpaidOrder.getId(), "Staff");

        assertEquals(OrderStatus.SELESAI, unpaidOrder.getStatus());
        assertEquals(PaymentStatus.BELUM_LUNAS, unpaidOrder.getPaymentStatus());
        assertEquals(BigDecimal.valueOf(16000).setScale(2), unpaidOrder.getRemainingAmount().setScale(2));

        // Advance to DIAMBIL with settlement
        Order settled = orderService.settlePaymentAndAdvanceToDiambil(
                unpaidOrder.getId(), BigDecimal.valueOf(16000), PaymentMethod.CASH, "Kasir 1");

        assertEquals(OrderStatus.DIAMBIL, settled.getStatus());
        assertEquals(PaymentStatus.LUNAS, settled.getPaymentStatus());
        assertEquals(BigDecimal.ZERO.setScale(2), settled.getRemainingAmount().setScale(2));
        assertEquals(BigDecimal.valueOf(16000).setScale(2), settled.getPaidAmount().setScale(2));
    }

    @Test
    void af3_editOrderDetailsWhenPending() {
        assertEquals(OrderStatus.PENDING, unpaidOrder.getStatus());

        List<OrderItem> newItems = List.of(new OrderItem(service, BigDecimal.valueOf(5.0), "Tambah baju"));
        Order updated = orderService.updateOrderItems(unpaidOrder.getId(), newItems, "Catatan baru");

        assertEquals(BigDecimal.valueOf(40000).setScale(2), updated.getTotalAmount().setScale(2));
    }

    @Test
    void af4_directPickupConfirmationWhenLunas() {
        orderService.advanceStatus(paidOrder.getId(), "Staff");
        orderService.advanceStatus(paidOrder.getId(), "Staff");
        assertEquals(OrderStatus.SELESAI, paidOrder.getStatus());
        assertEquals(PaymentStatus.LUNAS, paidOrder.getPaymentStatus());

        Order finalOrder = orderService.advanceStatus(paidOrder.getId(), "Staff");
        assertEquals(OrderStatus.DIAMBIL, finalOrder.getStatus());
    }

    @Test
    void br01_workflowProgressionStrict() {
        assertEquals(OrderStatus.PENDING, paidOrder.getStatus());
        Order step1 = orderService.advanceStatus(paidOrder.getId(), "Kasir");
        assertEquals(OrderStatus.PROSES, step1.getStatus());
        Order step2 = orderService.advanceStatus(step1.getId(), "Kasir");
        assertEquals(OrderStatus.SELESAI, step2.getStatus());
    }

    @Test
    void br03_diambilCannotAdvanceFurther() {
        orderService.advanceStatus(paidOrder.getId(), "Staff");
        orderService.advanceStatus(paidOrder.getId(), "Staff");
        orderService.advanceStatus(paidOrder.getId(), "Staff");
        assertEquals(OrderStatus.DIAMBIL, paidOrder.getStatus());

        assertThrows(IllegalStateException.class, () -> {
            orderService.advanceStatus(paidOrder.getId(), "Staff");
        });
    }

    @Test
    void br04_advanceToDiambilFailsIfUnpaidWithoutSettlement() {
        orderService.advanceStatus(unpaidOrder.getId(), "Staff");
        orderService.advanceStatus(unpaidOrder.getId(), "Staff");
        assertEquals(OrderStatus.SELESAI, unpaidOrder.getStatus());
        assertEquals(PaymentStatus.BELUM_LUNAS, unpaidOrder.getPaymentStatus());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            orderService.advanceStatus(unpaidOrder.getId(), "Staff");
        });
        assertTrue(ex.getMessage().contains("belum lunas"));
    }

    @Test
    void br05_cannotEditItemsIfOrderNotInPending() {
        orderService.advanceStatus(paidOrder.getId(), "Staff"); // Now PROSES
        assertEquals(OrderStatus.PROSES, paidOrder.getStatus());

        assertThrows(IllegalStateException.class, () -> {
            List<OrderItem> newItems = List.of(new OrderItem(service, BigDecimal.valueOf(1.0), ""));
            orderService.updateOrderItems(paidOrder.getId(), newItems, "");
        });
    }

    @Test
    void br06_statusTransitionsRecordUserAndTimestamp() {
        Order proses = orderService.advanceStatus(paidOrder.getId(), "Budi Operator");
        assertEquals("Budi Operator", proses.getStatusUpdatedBy());
        assertNotNull(proses.getStatusUpdatedAt());
    }
}
