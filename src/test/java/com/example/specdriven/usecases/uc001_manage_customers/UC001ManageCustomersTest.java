package com.example.specdriven.usecases.uc001_manage_customers;

import com.example.domain.Customer;
import com.example.domain.Order;
import com.example.domain.PaymentMethod;
import com.example.repository.CustomerRepository;
import com.example.repository.OrderRepository;
import com.example.service.CustomerService;
import com.example.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class UC001ManageCustomersTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        sampleCustomer = customerService.createCustomer("Rian Wijaya", "081987654321", "Jl. Anggrek No. 12");
    }

    @Test
    void mainFlow_createCustomerSuccessfully() {
        Customer created = customerService.createCustomer("Dewi Lestari", "081233445566", "Jl. Cempaka No. 4");
        assertNotNull(created.getId());
        assertEquals("Dewi Lestari", created.getName());
        assertEquals("081233445566", created.getPhone());
        assertEquals("Jl. Cempaka No. 4", created.getAddress());

        assertTrue(customerService.findById(created.getId()).isPresent());
    }

    @Test
    void af1_editExistingCustomerDetails() {
        Customer updated = customerService.updateCustomer(
                sampleCustomer.getId(), "Rian Wijaya Updated", "081987654399", "Jl. Anggrek Baru No. 15");
        assertEquals("Rian Wijaya Updated", updated.getName());
        assertEquals("081987654399", updated.getPhone());
        assertEquals("Jl. Anggrek Baru No. 15", updated.getAddress());
    }

    @Test
    void af2_viewCustomerOrderHistory() {
        List<Order> history = orderService.findByCustomer(sampleCustomer);
        assertNotNull(history);
    }

    @Test
    void af3_quickActionWhatsAppUrl() {
        String url = CustomerService.buildWhatsAppUrl("081234567890", "Halo Pelanggan");
        assertTrue(url.startsWith("https://wa.me/6281234567890?text="));
    }

    @Test
    void af4_quickActionPhoneCallSanitization() {
        String clean = CustomerService.cleanPhoneNumber("0812-3456-7890");
        assertEquals("081234567890", clean);
    }

    @Test
    void af5_duplicatePhoneNumberValidation() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            customerService.createCustomer("Orang Lain", sampleCustomer.getPhone(), "Jl. Lain");
        });
        assertTrue(ex.getMessage().contains("Nomor telepon sudah terdaftar"));
    }

    @Test
    void af6_deleteCustomerWithoutOrders() {
        Customer newCust = customerService.createCustomer("Bisa Dihapus", "081199887766", "Alamat");
        Long id = newCust.getId();
        customerService.deleteCustomer(id);
        assertFalse(customerService.findById(id).isPresent());
    }

    @Test
    void br01_nameValidation() {
        assertThrows(IllegalArgumentException.class, () -> customerService.createCustomer("A", "081234567801", ""));
        assertThrows(IllegalArgumentException.class, () -> customerService.createCustomer("", "081234567801", ""));
        assertThrows(IllegalArgumentException.class, () -> customerService.createCustomer(null, "081234567801", ""));
    }

    @Test
    void br02_phoneMandatoryAndFormat() {
        assertThrows(IllegalArgumentException.class, () -> customerService.createCustomer("Valid Name", "", ""));
        assertThrows(IllegalArgumentException.class, () -> customerService.createCustomer("Valid Name", "abc12345", ""));
        assertThrows(IllegalArgumentException.class, () -> customerService.createCustomer("Valid Name", "123", ""));
    }

    @Test
    void br03_addressIsOptional() {
        Customer cust = customerService.createCustomer("Tanpa Alamat", "081277889900", null);
        assertNotNull(cust.getId());
        assertEquals("", cust.getAddress());
    }

    @Test
    void br04_cannotDeleteCustomerWithExistingOrders() {
        // Create an order for sampleCustomer
        Order order = new Order();
        order.setCustomer(sampleCustomer);
        order.setInvoiceNumber("TEST-INV-001");
        order.setOrderDate(LocalDateTime.now());
        order.setEstimatedCompletionDate(LocalDateTime.now().plusHours(24));
        order.setTotalAmount(BigDecimal.valueOf(50000));
        order.setPaidAmount(BigDecimal.valueOf(50000));
        order.setRemainingAmount(BigDecimal.ZERO);
        order.setPaymentMethod(PaymentMethod.CASH);
        orderRepository.save(order);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            customerService.deleteCustomer(sampleCustomer.getId());
        });
        assertTrue(ex.getMessage().contains("riwayat transaksi"));
    }

    @Test
    void br05_whatsappPhoneSanitizationLeadingZeroTo62() {
        assertEquals("6281234567890", CustomerService.formatWhatsAppNumber("081234567890"));
        assertEquals("6281234567890", CustomerService.formatWhatsAppNumber("6281234567890"));
        assertEquals("6281234567890", CustomerService.formatWhatsAppNumber("+62 812-3456-7890"));
    }

    @Test
    void br06_customerSearchByNameOrPhone() {
        List<Customer> byName = customerService.search("Rian");
        assertFalse(byName.isEmpty());
        assertTrue(byName.stream().anyMatch(c -> c.getName().contains("Rian")));

        List<Customer> byPhone = customerService.search("081987654321");
        assertFalse(byPhone.isEmpty());
        assertTrue(byPhone.stream().anyMatch(c -> c.getPhone().equals("081987654321")));
    }
}
