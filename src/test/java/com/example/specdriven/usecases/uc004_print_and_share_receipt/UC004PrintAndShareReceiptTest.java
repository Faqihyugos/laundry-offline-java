package com.example.specdriven.usecases.uc004_print_and_share_receipt;

import com.example.domain.*;
import com.example.service.CustomerService;
import com.example.service.OrderService;
import com.example.service.ReceiptService;
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
public class UC004PrintAndShareReceiptTest {

    @Autowired
    private ReceiptService receiptService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ServicePackageService servicePackageService;

    private Order sampleOrder;
    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = customerService.createCustomer("Pak Joko", "081234567800", "Jl. Melati 3");
        LaundryService service = servicePackageService.createService("Cuci Kering UC004", BigDecimal.valueOf(10000), ServiceUnit.KG, 24, true);

        List<OrderItem> items = List.of(new OrderItem(service, BigDecimal.valueOf(3.0), ""));
        sampleOrder = orderService.createOrder(customer, null, items, LocalDateTime.now().plusDays(1), PaymentMethod.CASH, BigDecimal.valueOf(30000), "Wangi lavender");
    }

    @Test
    void subflowA_thermalReceiptGeneration() {
        String receipt58 = receiptService.generateThermalReceipt(sampleOrder, "58mm");
        assertNotNull(receipt58);
        assertTrue(receipt58.contains(sampleOrder.getInvoiceNumber()));
        assertTrue(receipt58.contains("Pak Joko"));
        assertTrue(receipt58.contains("TOTAL"));
        assertTrue(receipt58.contains("LUNAS"));

        String receipt80 = receiptService.generateThermalReceipt(sampleOrder, "80mm");
        assertNotNull(receipt80);
        assertTrue(receipt80.contains(sampleOrder.getInvoiceNumber()));
    }

    @Test
    void subflowB_whatsappDigitalReceiptFormatting() {
        String message = receiptService.generateWhatsAppMessage(sampleOrder);
        assertNotNull(message);
        assertTrue(message.contains("*STRUK DIGITAL LAUNDRY"));
        assertTrue(message.contains(sampleOrder.getInvoiceNumber()));
        assertTrue(message.contains("Pak Joko"));
        assertTrue(message.contains("*Total*"));
        assertTrue(message.contains("*Status Bayar*: LUNAS"));

        String waUrl = receiptService.generateWhatsAppUrl(sampleOrder);
        assertTrue(waUrl.startsWith("https://wa.me/6281234567800?text="));
    }

    @Test
    void af1_customerWithoutPhoneThrowsException() {
        Customer noPhoneCustomer = new Customer("Tanpa Nomor", "", "Alamat");
        Order orderWithoutPhone = new Order();
        orderWithoutPhone.setCustomer(noPhoneCustomer);

        assertThrows(IllegalStateException.class, () -> {
            receiptService.generateWhatsAppUrl(orderWithoutPhone);
        });
    }

    @Test
    void af2_paperWidthToggle() {
        String r58 = receiptService.generateThermalReceipt(sampleOrder, "58mm");
        String r80 = receiptService.generateThermalReceipt(sampleOrder, "80mm");
        assertNotEquals(r58, r80);
        // 80mm lines have length 48, 58mm lines have length 32
        assertTrue(r58.contains("-".repeat(32)));
        assertTrue(r80.contains("-".repeat(48)));
    }

    @Test
    void af3_historicalReceiptReprint() {
        // Advance status to SELESAI
        orderService.advanceStatus(sampleOrder.getId(), "Staff");
        orderService.advanceStatus(sampleOrder.getId(), "Staff");

        Order reloaded = orderService.findById(sampleOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.SELESAI, reloaded.getStatus());

        String receipt = receiptService.generateThermalReceipt(reloaded, "58mm");
        assertTrue(receipt.contains("SELESAI"));
    }

    @Test
    void br01_receiptFormattingCharactersWidth() {
        String r58 = receiptService.generateThermalReceipt(sampleOrder, "58mm");
        String r80 = receiptService.generateThermalReceipt(sampleOrder, "80mm");
        assertTrue(r58.contains("-".repeat(32)));
        assertTrue(r80.contains("-".repeat(48)));
    }

    @Test
    void br02_receiptProminentlyDisplaysKeyDetails() {
        String receipt = receiptService.generateThermalReceipt(sampleOrder, "58mm");
        assertTrue(receipt.contains("No. Inv  : " + sampleOrder.getInvoiceNumber()));
        assertTrue(receipt.contains("STATUS BAYAR"));
        assertTrue(receipt.contains("Estimasi Seles:"));
    }

    @Test
    void br03_whatsappUrlEncodesProperly() {
        String url = receiptService.generateWhatsAppUrl(sampleOrder);
        assertFalse(url.contains(" "));
        assertTrue(url.startsWith("https://wa.me/6281234567800?text="));
    }

    @Test
    void br04_phoneSanitizationConverts08To628() {
        String url = receiptService.generateWhatsAppUrl(sampleOrder);
        assertTrue(url.contains("wa.me/6281234567800"));
    }

    @Test
    void br05_reprintableAtAnyPoint() {
        assertNotNull(receiptService.generateThermalReceipt(sampleOrder, "58mm"));
        orderService.advanceStatus(sampleOrder.getId(), "Staff");
        assertNotNull(receiptService.generateThermalReceipt(sampleOrder, "58mm"));
    }
}
