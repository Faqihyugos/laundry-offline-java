package com.example.service;

import com.example.domain.AppSetting;
import com.example.repository.AppSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class SettingService {

    public static final String KEY_LAUNDRY_NAME = "laundry_name";
    public static final String KEY_LAUNDRY_ADDRESS = "laundry_address";
    public static final String KEY_LAUNDRY_PHONE = "laundry_phone";
    public static final String KEY_INVOICE_PREFIX = "invoice_prefix";
    public static final String KEY_PRINTER_PAPER_SIZE = "printer_paper_size";

    public static final String DEFAULT_LAUNDRY_NAME = "Laundry Bersih Wangi";
    public static final String DEFAULT_LAUNDRY_ADDRESS = "Jl. Sudirman No. 123, Jakarta";
    public static final String DEFAULT_LAUNDRY_PHONE = "6281234567890";
    public static final String DEFAULT_INVOICE_PREFIX = "LBW";
    public static final String DEFAULT_PRINTER_PAPER_SIZE = "58mm";

    private final AppSettingRepository settingRepository;

    public SettingService(AppSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    public String getSetting(String key, String defaultValue) {
        return settingRepository.findBySettingKey(key)
                .map(AppSetting::getSettingValue)
                .filter(val -> val != null && !val.trim().isEmpty())
                .orElse(defaultValue);
    }

    public void saveSetting(String key, String value) {
        AppSetting setting = settingRepository.findBySettingKey(key)
                .orElseGet(() -> new AppSetting(key, value));
        setting.setSettingValue(value != null ? value.trim() : "");
        settingRepository.save(setting);
    }

    public Map<String, String> getAllSettings() {
        Map<String, String> map = new HashMap<>();
        map.put(KEY_LAUNDRY_NAME, getSetting(KEY_LAUNDRY_NAME, DEFAULT_LAUNDRY_NAME));
        map.put(KEY_LAUNDRY_ADDRESS, getSetting(KEY_LAUNDRY_ADDRESS, DEFAULT_LAUNDRY_ADDRESS));
        map.put(KEY_LAUNDRY_PHONE, getSetting(KEY_LAUNDRY_PHONE, DEFAULT_LAUNDRY_PHONE));
        map.put(KEY_INVOICE_PREFIX, getSetting(KEY_INVOICE_PREFIX, DEFAULT_INVOICE_PREFIX));
        map.put(KEY_PRINTER_PAPER_SIZE, getSetting(KEY_PRINTER_PAPER_SIZE, DEFAULT_PRINTER_PAPER_SIZE));
        return map;
    }

    public void saveAllSettings(String laundryName, String address, String phone, String prefix, String paperSize) {
        if (laundryName == null || laundryName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nama laundry wajib diisi.");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Nomor telepon laundry wajib diisi.");
        }
        if (prefix == null || !prefix.trim().toUpperCase().matches("^[A-Z0-9]{2,6}$")) {
            throw new IllegalArgumentException("Prefix invoice harus berupa 2-6 karakter huruf/angka tanpa spasi (contoh: LBW, INV).");
        }
        String cleanPaper = (paperSize != null && paperSize.equalsIgnoreCase("80mm")) ? "80mm" : "58mm";

        saveSetting(KEY_LAUNDRY_NAME, laundryName.trim());
        saveSetting(KEY_LAUNDRY_ADDRESS, address != null ? address.trim() : "");
        saveSetting(KEY_LAUNDRY_PHONE, phone.trim());
        saveSetting(KEY_INVOICE_PREFIX, prefix.trim().toUpperCase());
        saveSetting(KEY_PRINTER_PAPER_SIZE, cleanPaper);
    }

    public void resetToDefaults() {
        saveSetting(KEY_LAUNDRY_NAME, DEFAULT_LAUNDRY_NAME);
        saveSetting(KEY_LAUNDRY_ADDRESS, DEFAULT_LAUNDRY_ADDRESS);
        saveSetting(KEY_LAUNDRY_PHONE, DEFAULT_LAUNDRY_PHONE);
        saveSetting(KEY_INVOICE_PREFIX, DEFAULT_INVOICE_PREFIX);
        saveSetting(KEY_PRINTER_PAPER_SIZE, DEFAULT_PRINTER_PAPER_SIZE);
    }

    public String generateTestReceipt(String paperSize) {
        boolean is80mm = "80mm".equalsIgnoreCase(paperSize);
        int width = is80mm ? 48 : 32;
        String line = "-".repeat(width);

        String shopName = getSetting(KEY_LAUNDRY_NAME, DEFAULT_LAUNDRY_NAME);
        String address = getSetting(KEY_LAUNDRY_ADDRESS, DEFAULT_LAUNDRY_ADDRESS);
        String phone = getSetting(KEY_LAUNDRY_PHONE, DEFAULT_LAUNDRY_PHONE);

        StringBuilder sb = new StringBuilder();
        sb.append(centerText(shopName, width)).append("\n");
        sb.append(centerText(address, width)).append("\n");
        sb.append(centerText("Telp: " + phone, width)).append("\n");
        sb.append(line).append("\n");
        sb.append(centerText("*** STRUK UJI COBA PRINTER ***", width)).append("\n");
        sb.append(line).append("\n");
        sb.append("Printer Size : ").append(paperSize).append("\n");
        sb.append("Format Status: NORMAL / OK\n");
        sb.append(line).append("\n");
        sb.append(padBetween("Item Uji Coba 1", "Rp 15.000", width)).append("\n");
        sb.append(padBetween("Item Uji Coba 2", "Rp 25.000", width)).append("\n");
        sb.append(line).append("\n");
        sb.append(padBetween("TOTAL", "Rp 40.000", width)).append("\n");
        sb.append(line).append("\n");
        sb.append(centerText("Terima Kasih - Pengujian Berhasil", width)).append("\n");
        return sb.toString();
    }

    public static String centerText(String text, int width) {
        if (text == null) return "";
        if (text.length() >= width) return text.substring(0, width);
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text;
    }

    public static String padBetween(String left, String right, int width) {
        if (left == null) left = "";
        if (right == null) right = "";
        int space = width - left.length() - right.length();
        if (space < 1) {
            return left + " " + right;
        }
        return left + " ".repeat(space) + right;
    }
}
