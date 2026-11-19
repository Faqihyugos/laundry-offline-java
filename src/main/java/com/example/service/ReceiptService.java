package com.example.service;

import com.example.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ReceiptService {

    private final SettingService settingService;

    public ReceiptService(SettingService settingService) {
        this.settingService = settingService;
    }

    public String generateThermalReceipt(Order order, String paperWidth) {
        boolean is80mm = "80mm".equalsIgnoreCase(paperWidth);
        int width = is80mm ? 48 : 32;
        String line = "-".repeat(width);

        String shopName = settingService.getSetting(SettingService.KEY_LAUNDRY_NAME, SettingService.DEFAULT_LAUNDRY_NAME);
        String address = settingService.getSetting(SettingService.KEY_LAUNDRY_ADDRESS, SettingService.DEFAULT_LAUNDRY_ADDRESS);
        String phone = settingService.getSetting(SettingService.KEY_LAUNDRY_PHONE, SettingService.DEFAULT_LAUNDRY_PHONE);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String orderDateStr = order.getOrderDate() != null ? order.getOrderDate().format(dtf) : "-";
        String estDateStr = order.getEstimatedCompletionDate() != null ? order.getEstimatedCompletionDate().format(dtf) : "-";

        StringBuilder sb = new StringBuilder();
        sb.append(SettingService.centerText(shopName, width)).append("\n");
        sb.append(SettingService.centerText(address, width)).append("\n");
        sb.append(SettingService.centerText("Telp/WA: " + phone, width)).append("\n");
        sb.append(line).append("\n");
        sb.append("No. Inv  : ").append(order.getInvoiceNumber()).append("\n");
        sb.append("Tgl Masuk: ").append(orderDateStr).append("\n");
        sb.append("Kasir    : ").append(order.getCashier() != null ? order.getCashier().getFullName() : "Staff").append("\n");
        sb.append("Pelanggan: ").append(order.getCustomer() != null ? order.getCustomer().getName() : "-").append("\n");
        if (order.getCustomer() != null && order.getCustomer().getPhone() != null) {
            sb.append("No. Telp : ").append(order.getCustomer().getPhone()).append("\n");
        }
        sb.append(line).append("\n");

        // Line items
        for (OrderItem item : order.getItems()) {
            String qtyStr = formatQuantity(item.getQuantity()) + " " + (item.getUnit() == ServiceUnit.KG ? "kg" : "pcs");
            String priceStr = "@" + formatRupiah(item.getUnitPrice());
            String subStr = formatRupiah(item.getSubtotal());

            sb.append(item.getServiceName()).append("\n");
            sb.append(SettingService.padBetween("  " + qtyStr + " " + priceStr, subStr, width)).append("\n");
            if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                sb.append("  (Catatan: ").append(item.getNotes().trim()).append(")\n");
            }
        }

        sb.append(line).append("\n");
        sb.append(SettingService.padBetween("TOTAL", formatRupiah(order.getTotalAmount()), width)).append("\n");
        sb.append(SettingService.padBetween("BAYAR (" + order.getPaymentMethod().name() + ")", formatRupiah(order.getPaidAmount()), width)).append("\n");

        if (order.getPaidAmount().compareTo(order.getTotalAmount()) > 0) {
            BigDecimal kembalian = order.getPaidAmount().subtract(order.getTotalAmount());
            sb.append(SettingService.padBetween("KEMBALIAN", formatRupiah(kembalian), width)).append("\n");
        } else {
            sb.append(SettingService.padBetween("SISA TAGIHAN", formatRupiah(order.getRemainingAmount()), width)).append("\n");
        }

        sb.append(SettingService.padBetween("STATUS BAYAR", order.getPaymentStatus().getDisplayName().toUpperCase(), width)).append("\n");
        sb.append(line).append("\n");
        sb.append("Status Cucian : ").append(order.getStatus().getDisplayName().toUpperCase()).append("\n");
        sb.append("Estimasi Seles: ").append(estDateStr).append("\n");
        if (order.getNotes() != null && !order.getNotes().trim().isEmpty()) {
            sb.append("Catatan: ").append(order.getNotes()).append("\n");
        }
        sb.append(line).append("\n");
        sb.append(SettingService.centerText("Terima kasih atas kunjungan Anda!", width)).append("\n");
        sb.append(SettingService.centerText("Barang hilang/rusak karena susut", width)).append("\n");
        sb.append(SettingService.centerText("bukan tanggung jawab laundry.", width)).append("\n");
        return sb.toString();
    }

    public String generateWhatsAppMessage(Order order) {
        String shopName = settingService.getSetting(SettingService.KEY_LAUNDRY_NAME, SettingService.DEFAULT_LAUNDRY_NAME);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String orderDateStr = order.getOrderDate() != null ? order.getOrderDate().format(dtf) : "-";
        String estDateStr = order.getEstimatedCompletionDate() != null ? order.getEstimatedCompletionDate().format(dtf) : "-";

        StringBuilder sb = new StringBuilder();
        sb.append("*STRUK DIGITAL LAUNDRY - ").append(shopName).append("*\n");
        sb.append("----------------------------------------\n");
        sb.append("No. Invoice : ").append(order.getInvoiceNumber()).append("\n");
        sb.append("Tanggal     : ").append(orderDateStr).append("\n");
        sb.append("Pelanggan   : ").append(order.getCustomer() != null ? order.getCustomer().getName() : "-").append("\n");
        sb.append("Status      : ").append(order.getStatus().getDisplayName()).append("\n");
        sb.append("Estimasi    : ").append(estDateStr).append("\n");
        sb.append("----------------------------------------\n");
        sb.append("*Rincian Layanan:*\n");

        for (OrderItem item : order.getItems()) {
            String qtyStr = formatQuantity(item.getQuantity()) + " " + (item.getUnit() == ServiceUnit.KG ? "kg" : "pcs");
            sb.append("• ").append(item.getServiceName())
              .append(" (").append(qtyStr).append(")")
              .append(" : ").append(formatRupiah(item.getSubtotal())).append("\n");
            if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                sb.append("  _Catatan: ").append(item.getNotes().trim()).append("_\n");
            }
        }

        sb.append("----------------------------------------\n");
        sb.append("*Total*       : ").append(formatRupiah(order.getTotalAmount())).append("\n");
        sb.append("*Bayar*       : ").append(formatRupiah(order.getPaidAmount()))
          .append(" (").append(order.getPaymentMethod().name()).append(")\n");
        sb.append("*Sisa*        : ").append(formatRupiah(order.getRemainingAmount())).append("\n");
        sb.append("*Status Bayar*: ").append(order.getPaymentStatus().getDisplayName().toUpperCase()).append("\n");
        sb.append("----------------------------------------\n");
        sb.append("Terima kasih telah mempercayakan pakaian Anda kepada kami!\n");
        return sb.toString();
    }

    public String generateWhatsAppUrl(Order order) {
        if (order.getCustomer() == null || order.getCustomer().getPhone() == null || order.getCustomer().getPhone().trim().isEmpty()) {
            throw new IllegalStateException("Pelanggan belum memiliki nomor telepon. Silakan lengkapi nomor telepon terlebih dahulu.");
        }
        String text = generateWhatsAppMessage(order);
        return CustomerService.buildWhatsAppUrl(order.getCustomer().getPhone(), text);
    }

    public static String formatRupiah(BigDecimal amount) {
        if (amount == null) return "Rp 0";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("id", "ID"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("Rp #,##0", symbols);
        return df.format(amount);
    }

    public static String formatQuantity(BigDecimal qty) {
        if (qty == null) return "0";
        if (qty.stripTrailingZeros().scale() <= 0) {
            return String.valueOf(qty.intValue());
        }
        return qty.stripTrailingZeros().toPlainString();
    }
}
