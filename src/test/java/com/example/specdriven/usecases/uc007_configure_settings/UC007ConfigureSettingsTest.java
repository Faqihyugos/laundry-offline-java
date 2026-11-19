package com.example.specdriven.usecases.uc007_configure_settings;

import com.example.service.SettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class UC007ConfigureSettingsTest {

    @Autowired
    private SettingService settingService;

    @BeforeEach
    void setUp() {
        settingService.resetToDefaults();
    }

    @Test
    void mainFlow_configureSettingsSuccessfully() {
        settingService.saveAllSettings(
                "Laundry Wangi Sejahtera",
                "Jl. Pahlawan No. 45, Bandung",
                "081299887766",
                "LWS",
                "80mm"
        );

        Map<String, String> settings = settingService.getAllSettings();
        assertEquals("Laundry Wangi Sejahtera", settings.get(SettingService.KEY_LAUNDRY_NAME));
        assertEquals("Jl. Pahlawan No. 45, Bandung", settings.get(SettingService.KEY_LAUNDRY_ADDRESS));
        assertEquals("081299887766", settings.get(SettingService.KEY_LAUNDRY_PHONE));
        assertEquals("LWS", settings.get(SettingService.KEY_INVOICE_PREFIX));
        assertEquals("80mm", settings.get(SettingService.KEY_PRINTER_PAPER_SIZE));
    }

    @Test
    void af1_testThermalPrinterConfiguration() {
        String testSlip58 = settingService.generateTestReceipt("58mm");
        assertNotNull(testSlip58);
        assertTrue(testSlip58.contains("STRUK UJI COBA PRINTER"));
        assertTrue(testSlip58.contains("58mm"));

        String testSlip80 = settingService.generateTestReceipt("80mm");
        assertNotNull(testSlip80);
        assertTrue(testSlip80.contains("80mm"));
    }

    @Test
    void af2_invalidInvoicePrefixValidation() {
        // Less than 2 chars
        assertThrows(IllegalArgumentException.class, () -> {
            settingService.saveAllSettings("Nama", "Alamat", "0812345678", "L", "58mm");
        });

        // More than 6 chars
        assertThrows(IllegalArgumentException.class, () -> {
            settingService.saveAllSettings("Nama", "Alamat", "0812345678", "TOOLONGPREFIX", "58mm");
        });

        // With spaces or special characters
        assertThrows(IllegalArgumentException.class, () -> {
            settingService.saveAllSettings("Nama", "Alamat", "0812345678", "LB W", "58mm");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            settingService.saveAllSettings("Nama", "Alamat", "0812345678", "LB-W", "58mm");
        });
    }

    @Test
    void af3_resetToDefaultSettings() {
        settingService.saveAllSettings("Nama Custom", "Alamat Custom", "0811111111", "CST", "80mm");
        settingService.resetToDefaults();

        Map<String, String> settings = settingService.getAllSettings();
        assertEquals(SettingService.DEFAULT_LAUNDRY_NAME, settings.get(SettingService.KEY_LAUNDRY_NAME));
        assertEquals(SettingService.DEFAULT_INVOICE_PREFIX, settings.get(SettingService.KEY_INVOICE_PREFIX));
        assertEquals(SettingService.DEFAULT_PRINTER_PAPER_SIZE, settings.get(SettingService.KEY_PRINTER_PAPER_SIZE));
    }

    @Test
    void br02_nameAndPhoneAreMandatory() {
        assertThrows(IllegalArgumentException.class, () -> {
            settingService.saveAllSettings("", "Alamat", "0812345678", "LBW", "58mm");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            settingService.saveAllSettings("Nama", "Alamat", "", "LBW", "58mm");
        });
    }

    @Test
    void br03_prefix2To6UppercaseAlphanumeric() {
        settingService.saveAllSettings("Nama", "Alamat", "0812345678", "inv01", "58mm");
        assertEquals("INV01", settingService.getSetting(SettingService.KEY_INVOICE_PREFIX, ""));
    }

    @Test
    void br05_supportedPaperWidths() {
        settingService.saveAllSettings("Nama", "Alamat", "0812345678", "LBW", "80mm");
        assertEquals("80mm", settingService.getSetting(SettingService.KEY_PRINTER_PAPER_SIZE, ""));

        settingService.saveAllSettings("Nama", "Alamat", "0812345678", "LBW", "invalidWidth");
        assertEquals("58mm", settingService.getSetting(SettingService.KEY_PRINTER_PAPER_SIZE, ""));
    }
}
