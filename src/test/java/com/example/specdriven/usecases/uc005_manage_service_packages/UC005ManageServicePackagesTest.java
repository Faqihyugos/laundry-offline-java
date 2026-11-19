package com.example.specdriven.usecases.uc005_manage_service_packages;

import com.example.domain.*;
import com.example.repository.CustomerRepository;
import com.example.repository.OrderItemRepository;
import com.example.repository.OrderRepository;
import com.example.service.CustomerService;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class UC005ManageServicePackagesTest {

    @Autowired
    private ServicePackageService servicePackageService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerService customerService;

    private LaundryService sampleService;

    @BeforeEach
    void setUp() {
        sampleService = servicePackageService.createService("Cuci Karpet Spesial", BigDecimal.valueOf(25000), ServiceUnit.PCS, 72, true);
    }

    @Test
    void mainFlow_createServicePackageSuccessfully() {
        LaundryService created = servicePackageService.createService("Dry Clean Jas", BigDecimal.valueOf(45000), ServiceUnit.PCS, 48, true);
        assertNotNull(created.getId());
        assertEquals("Dry Clean Jas", created.getName());
        assertEquals(BigDecimal.valueOf(45000), created.getPrice());
        assertEquals(ServiceUnit.PCS, created.getUnit());
        assertEquals(48, created.getEstimatedDurationHours());
        assertTrue(created.isActive());
    }

    @Test
    void af1_editExistingServicePackage() {
        LaundryService updated = servicePackageService.updateService(
                sampleService.getId(), "Cuci Karpet Super", BigDecimal.valueOf(30000), ServiceUnit.PCS, 96, true);
        assertEquals("Cuci Karpet Super", updated.getName());
        assertEquals(BigDecimal.valueOf(30000), updated.getPrice());
        assertEquals(96, updated.getEstimatedDurationHours());
    }

    @Test
    void af2_toggleServiceActiveStatus() {
        assertTrue(sampleService.isActive());

        LaundryService toggled = servicePackageService.toggleActive(sampleService.getId());
        assertFalse(toggled.isActive());

        LaundryService toggledBack = servicePackageService.toggleActive(sampleService.getId());
        assertTrue(toggledBack.isActive());
    }

    @Test
    void af3_deleteServicePackageWithoutOrders() {
        LaundryService s = servicePackageService.createService("Layanan Sementara", BigDecimal.valueOf(15000), ServiceUnit.KG, 24, true);
        Long id = s.getId();
        servicePackageService.deleteService(id);
        assertFalse(servicePackageService.findById(id).isPresent());
    }

    @Test
    void af4_validationFailureInvalidPriceOrMissingName() {
        assertThrows(IllegalArgumentException.class, () -> {
            servicePackageService.createService("", BigDecimal.valueOf(10000), ServiceUnit.KG, 24, true);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            servicePackageService.createService("Tarif Nol", BigDecimal.ZERO, ServiceUnit.KG, 24, true);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            servicePackageService.createService("Tarif Negatif", BigDecimal.valueOf(-5000), ServiceUnit.KG, 24, true);
        });
    }

    @Test
    void br01_serviceNameUnique() {
        assertThrows(IllegalArgumentException.class, () -> {
            servicePackageService.createService("Cuci Karpet Spesial", BigDecimal.valueOf(20000), ServiceUnit.PCS, 48, true);
        });
    }

    @Test
    void br02_pricePositive() {
        assertThrows(IllegalArgumentException.class, () -> {
            servicePackageService.createService("Test Price", BigDecimal.valueOf(-1), ServiceUnit.KG, 24, true);
        });
    }

    @Test
    void br03_unitMustBeValid() {
        assertThrows(IllegalArgumentException.class, () -> {
            servicePackageService.createService("No Unit", BigDecimal.valueOf(10000), null, 24, true);
        });
    }

    @Test
    void br04_durationMustBeGreaterThanZero() {
        assertThrows(IllegalArgumentException.class, () -> {
            servicePackageService.createService("Zero Duration", BigDecimal.valueOf(10000), ServiceUnit.KG, 0, true);
        });
    }

    @Test
    void br05_inactiveServicesHiddenFromActiveList() {
        servicePackageService.toggleActive(sampleService.getId());
        List<LaundryService> active = servicePackageService.findActive();
        assertFalse(active.stream().anyMatch(s -> s.getId().equals(sampleService.getId())));
    }

    @Test
    void br06_cannotDeleteServiceUsedInOrders() {
        Customer c = customerService.createCustomer("Pelanggan Layanan", "081999888222", "");
        List<OrderItem> items = List.of(new OrderItem(sampleService, BigDecimal.valueOf(1), ""));
        orderService.createOrder(c, null, items, LocalDateTime.now().plusDays(1), PaymentMethod.CASH, BigDecimal.ZERO, "");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            servicePackageService.deleteService(sampleService.getId());
        });
        assertTrue(ex.getMessage().contains("sudah digunakan"));
    }
}
