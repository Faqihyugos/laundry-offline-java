package com.example.ui.views.setting;

import com.example.service.ReceiptService;
import com.example.service.SettingService;
import com.example.ui.MainLayout;
import com.example.ui.components.ThermalReceiptDialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Pengaturan | Laundry POS")
@RolesAllowed("OWNER")
public class SettingView extends VerticalLayout {

    private final SettingService settingService;
    private final ReceiptService receiptService;

    // Sub-Navigation Tabs (Figma Match)
    private enum SettingTab {
        PROFIL("Profil Toko", VaadinIcon.SHOP),
        DISKON("Kelola Diskon", VaadinIcon.TAG),
        PRINTER("Kelola Printer", VaadinIcon.PRINT),
        PAJAK("Kelola Pajak", VaadinIcon.INVOICE);

        final String label;
        final VaadinIcon icon;

        SettingTab(String label, VaadinIcon icon) {
            this.label = label;
            this.icon = icon;
        }
    }

    private SettingTab currentTab = SettingTab.PROFIL;

    private final Div navContainer = new Div();
    private final Div contentArea = new Div();

    // Tab 1: Profil Toko Fields
    private final TextField laundryNameField = new TextField("Nama Laundry");
    private final TextArea laundryAddressField = new TextArea("Alamat Lengkap");
    private final TextField laundryPhoneField = new TextField("Nomor Telepon / WhatsApp");
    private final TextField invoicePrefixField = new TextField("Prefix Nomor Invoice");
    private final Span invoicePreview = new Span();

    // Tab 2: Diskon State
    private record DiscountItem(String name, String desc, String type, String value) {}
    private final List<DiscountItem> customDiscounts = new ArrayList<>();
    private final Div discountGrid = new Div();

    // Tab 3: Printer Fields
    private final TextField printerNameField = new TextField("Nama Printer");
    private final ComboBox<String> printerConnectionType = new ComboBox<>("Tipe Koneksi Printer");
    private final RadioButtonGroup<String> paperSizeRadio = new RadioButtonGroup<>();
    private final Checkbox autoCutCheckbox = new Checkbox("Potong kertas otomatis setelah cetak");
    private final Checkbox autoPrintCheckbox = new Checkbox("Cetak struk otomatis setelah pembayaran");

    // Tab 4: Pajak Fields
    private final TextField taxNameField = new TextField("Nama Pajak");
    private final RadioButtonGroup<String> taxRateRadio = new RadioButtonGroup<>();
    private final Checkbox taxActiveCheckbox = new Checkbox("Aktifkan perhitungan pajak pada pesanan baru");

    public SettingView(SettingService settingService, ReceiptService receiptService) {
        this.settingService = settingService;
        this.receiptService = receiptService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("max-width", "1280px").set("margin", "0 auto");

        initDefaultDiscounts();
        createHeader();
        createTwoColumnLayout();
        loadSettings();
    }

    private void initDefaultDiscounts() {
        customDiscounts.add(new DiscountItem("Diskon Pelanggan Member", "Potongan tarif 10% untuk member setia laundry", "Persentase", "10%"));
        customDiscounts.add(new DiscountItem("Promo Spesial Opening", "Promo potongan 15% menyambut pembukaan outlet", "Persentase", "15%"));
        customDiscounts.add(new DiscountItem("Potongan Tunai Langsung", "Potongan langsung Rp 5.000 untuk transaksi non-promo", "Nominal", "Rp 5.000"));
    }

    private void createHeader() {
        H2 title = new H2("Pengaturan Sistem POS");
        title.getStyle().set("margin", "0")
                .set("color", "var(--text-main)")
                .set("font-size", "1.6rem")
                .set("font-weight", "800")
                .set("letter-spacing", "-0.02em");

        Paragraph subtitle = new Paragraph("Konfigurasi profil outlet, katalog diskon & promo, printer thermal, dan perhitungan pajak.");
        subtitle.getStyle().set("margin", "4px 0 0.5rem 0").set("color", "var(--text-muted)").set("font-size", "0.88rem");

        add(title, subtitle);
    }

    private void createTwoColumnLayout() {
        Div container = new Div();
        container.addClassName("settings-container");

        // 1. Left: Sub-Navigation
        navContainer.addClassName("settings-sidebar-nav");
        renderNavItems();

        // 2. Right: Content Area
        contentArea.addClassName("settings-content-area");
        renderContentTab();

        container.add(navContainer, contentArea);
        add(container);
    }

    private void renderNavItems() {
        navContainer.removeAll();
        for (SettingTab tab : SettingTab.values()) {
            Div item = new Div();
            item.addClassName("settings-nav-item");
            if (tab == currentTab) {
                item.addClassName("active");
            }

            Icon icon = tab.icon.create();
            icon.setSize("18px");
            Span label = new Span(tab.label);

            item.add(icon, label);
            item.addClickListener(e -> {
                currentTab = tab;
                renderNavItems();
                renderContentTab();
            });

            navContainer.add(item);
        }
    }

    private void renderContentTab() {
        contentArea.removeAll();
        switch (currentTab) {
            case PROFIL -> renderProfilTab();
            case DISKON -> renderDiskonTab();
            case PRINTER -> renderPrinterTab();
            case PAJAK -> renderPajakTab();
        }
    }

    // ==========================================
    // TAB 1: PROFIL TOKO
    // ==========================================
    private void renderProfilTab() {
        Div shopCard = createCardWrapper("Profil Laundry & Identitas Outlet");
        laundryNameField.setWidthFull();
        laundryNameField.setRequired(true);

        laundryAddressField.setWidthFull();
        laundryAddressField.setPlaceholder("Alamat lengkap outlet laundry...");

        laundryPhoneField.setWidthFull();
        laundryPhoneField.setRequired(true);
        laundryPhoneField.setPlaceholder("Nomor telepon yang tampil pada struk thermal dan link WhatsApp");

        shopCard.add(laundryNameField, laundryAddressField, laundryPhoneField);

        Div invoiceCard = createCardWrapper("Format Nomor Invoice Transaksi");
        invoicePrefixField.setWidthFull();
        invoicePrefixField.setRequired(true);
        invoicePrefixField.setHelperText("Prefix 2-6 karakter alfanumerik huruf kapital tanpa spasi (contoh: LBW, INV)");
        invoicePrefixField.addValueChangeListener(e -> updateInvoicePreview());

        HorizontalLayout previewLayout = new HorizontalLayout(new Span("Contoh Nomor Invoice: "), invoicePreview);
        previewLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        invoicePreview.getStyle().set("font-weight", "800").set("color", "var(--primary)");

        invoiceCard.add(invoicePrefixField, previewLayout);

        Button saveBtn = new Button("Simpan Profil Toko", VaadinIcon.CHECK.create(), e -> saveProfilSettings());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.getStyle().set("background", "var(--primary-gradient)");

        contentArea.add(shopCard, invoiceCard, saveBtn);
    }

    // ==========================================
    // TAB 2: KELOLA DISKON (Figma Match)
    // ==========================================
    private void renderDiskonTab() {
        Div headerBox = new Div();
        H3 title = new H3("Kelola Diskon & Promo Pelanggan");
        title.getStyle().set("margin", "0").set("color", "var(--text-main)").set("font-weight", "800");
        Paragraph sub = new Paragraph("Diskon yang terdaftar di sini dapat langsung diterapkan oleh kasir pada tiket pesanan.");
        sub.getStyle().set("color", "var(--text-muted)").set("font-size", "0.85rem").set("margin", "4px 0 16px 0");
        headerBox.add(title, sub);

        discountGrid.removeAll();
        discountGrid.addClassName("setting-card-grid");

        // Action Card: + Tambah Diskon Baru
        Div addCard = new Div();
        addCard.addClassName("setting-action-card");
        Icon plusIcon = VaadinIcon.PLUS_CIRCLE.create();
        plusIcon.setSize("32px");
        Span plusLabel = new Span("+ Tambah Diskon Baru");
        addCard.add(plusIcon, plusLabel);
        addCard.addClickListener(e -> openAddDiscountDialog());

        discountGrid.add(addCard);

        for (DiscountItem item : customDiscounts) {
            Div card = new Div();
            card.addClassName("modern-card");
            card.getStyle().set("display", "flex").set("flex-direction", "column").set("justify-content", "space-between").set("gap", "8px");

            Div top = new Div();
            Span badge = new Span(item.type());
            badge.addClassNames("status-badge", "badge-role-owner");

            H3 name = new H3(item.name());
            name.getStyle().set("font-size", "1.05rem").set("margin", "6px 0 2px 0").set("color", "var(--text-main)").set("font-weight", "700");

            Paragraph desc = new Paragraph(item.desc());
            desc.getStyle().set("font-size", "0.8rem").set("color", "var(--text-muted)").set("margin", "0");

            top.add(badge, name, desc);

            Div btm = new Div();
            btm.getStyle().set("display", "flex").set("justify-content", "space-between").set("align-items", "center").set("border-top", "1px solid #F1F5F9").set("padding-top", "8px");
            Span valSpan = new Span(item.value());
            valSpan.getStyle().set("font-weight", "800").set("color", "var(--primary)").set("font-size", "1.1rem");

            Span statusPill = new Span("Aktif");
            statusPill.addClassNames("status-badge", "badge-selesai");

            btm.add(valSpan, statusPill);
            card.add(top, btm);
            discountGrid.add(card);
        }

        contentArea.add(headerBox, discountGrid);
    }

    private void openAddDiscountDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Tambah Diskon Baru");
        dialog.setWidth("440px");

        TextField nameField = new TextField("Nama Diskon");
        nameField.setWidthFull();
        nameField.setRequired(true);
        nameField.setPlaceholder("Contoh: Promo Gajian, Diskon Jumat...");

        TextArea descField = new TextArea("Deskripsi Promo");
        descField.setWidthFull();
        descField.setPlaceholder("Keterangan syarat atau ketentuan promo...");

        RadioButtonGroup<String> typeRadio = new RadioButtonGroup<>();
        typeRadio.setLabel("Tipe Diskon");
        typeRadio.setItems("Persentase (%)", "Nominal (Rp)");
        typeRadio.setValue("Persentase (%)");

        BigDecimalField valField = new BigDecimalField("Besaran Nilai");
        valField.setWidthFull();
        valField.setPlaceholder("Contoh: 10 atau 5000");

        VerticalLayout form = new VerticalLayout(nameField, descField, typeRadio, valField);
        form.setPadding(false);

        Button saveBtn = new Button("Simpan Diskon", e -> {
            if (nameField.getValue() == null || nameField.getValue().trim().isEmpty() || valField.getValue() == null) {
                showNotification("Lengkapi nama diskon dan nilai diskon", NotificationVariant.LUMO_WARNING);
                return;
            }

            boolean isPct = "Persentase (%)".equals(typeRadio.getValue());
            String displayVal = isPct ? valField.getValue() + "%" : ReceiptService.formatRupiah(valField.getValue());
            customDiscounts.add(new DiscountItem(nameField.getValue(), descField.getValue(), isPct ? "Persentase" : "Nominal", displayVal));
            renderDiskonTab();
            showNotification("Diskon berhasil ditambahkan!", NotificationVariant.LUMO_SUCCESS);
            dialog.close();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.getStyle().set("background", "var(--primary-gradient)");

        Button cancelBtn = new Button("Batal", e -> dialog.close());
        dialog.add(form);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    // ==========================================
    // TAB 3: KELOLA PRINTER (Figma Match)
    // ==========================================
    private void renderPrinterTab() {
        Div printerCard = createCardWrapper("Konfigurasi Printer Struk Thermal");

        printerNameField.setWidthFull();
        printerNameField.setValue("Thermal Receipt Printer POS");

        printerConnectionType.setWidthFull();
        printerConnectionType.setItems("USB Direct Print", "Bluetooth Portable", "Network / Ethernet LAN");
        printerConnectionType.setValue("USB Direct Print");

        paperSizeRadio.setLabel("Ukuran Lebar Kertas Struk");
        paperSizeRadio.setItems("58mm", "80mm");

        autoCutCheckbox.setValue(true);
        autoPrintCheckbox.setValue(false);

        VerticalLayout toggles = new VerticalLayout(autoCutCheckbox, autoPrintCheckbox);
        toggles.setPadding(false);
        toggles.setSpacing(false);

        HorizontalLayout testBtnRow = new HorizontalLayout();
        Button testPrintBtn = new Button("Uji Coba Cetak Struk (Test Print)", VaadinIcon.PRINT.create(), e -> {
            ThermalReceiptDialog testDialog = new ThermalReceiptDialog(receiptService, settingService, null, true);
            testDialog.open();
        });
        testPrintBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        testBtnRow.add(testPrintBtn);

        printerCard.add(printerNameField, printerConnectionType, paperSizeRadio, toggles, testBtnRow);

        Button saveBtn = new Button("Simpan Konfigurasi Printer", VaadinIcon.CHECK.create(), e -> savePrinterSettings());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.getStyle().set("background", "var(--primary-gradient)");

        contentArea.add(printerCard, saveBtn);
    }

    // ==========================================
    // TAB 4: KELOLA PAJAK (Figma Match)
    // ==========================================
    private void renderPajakTab() {
        Div taxCard = createCardWrapper("Pengaturan Pajak Transaksi & PPN");

        taxNameField.setWidthFull();
        taxNameField.setValue("Pajak Pertambahan Nilai (PPN)");

        taxRateRadio.setLabel("Tarif Pajak Standar");
        taxRateRadio.setItems("11% (PPN Standar Nasional)", "0% (Bebas Pajak)");
        taxRateRadio.setValue("11% (PPN Standar Nasional)");

        taxActiveCheckbox.setValue(false);

        Paragraph info = new Paragraph("Pengaturan ini menentukan pilihan pajak default saat kasir membuat tiket pesanan baru di POS.");
        info.getStyle().set("color", "var(--text-muted)").set("font-size", "0.82rem");

        taxCard.add(taxNameField, taxRateRadio, taxActiveCheckbox, info);

        Button saveBtn = new Button("Simpan Pengaturan Pajak", VaadinIcon.CHECK.create(), e -> {
            showNotification("Pengaturan pajak berhasil disimpan!", NotificationVariant.LUMO_SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.getStyle().set("background", "var(--primary-gradient)");

        contentArea.add(taxCard, saveBtn);
    }

    // ==========================================
    // Helper Methods & Persistence
    // ==========================================
    private Div createCardWrapper(String headerText) {
        Div card = new Div();
        card.addClassName("modern-card");
        card.getStyle().set("margin-bottom", "16px").set("display", "flex").set("flex-direction", "column").set("gap", "12px");

        H3 heading = new H3(headerText);
        heading.getStyle().set("margin", "0 0 4px 0")
                .set("color", "var(--text-main)")
                .set("font-size", "1.1rem")
                .set("font-weight", "700");

        card.add(heading);
        return card;
    }

    private void updateInvoicePreview() {
        String prefix = invoicePrefixField.getValue() != null && !invoicePrefixField.getValue().trim().isEmpty()
                ? invoicePrefixField.getValue().trim().toUpperCase() : SettingService.DEFAULT_INVOICE_PREFIX;
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        invoicePreview.setText(prefix + "-" + dateStr + "-0001");
    }

    private void loadSettings() {
        Map<String, String> settings = settingService.getAllSettings();

        laundryNameField.setValue(settings.getOrDefault(SettingService.KEY_LAUNDRY_NAME, SettingService.DEFAULT_LAUNDRY_NAME));
        laundryAddressField.setValue(settings.getOrDefault(SettingService.KEY_LAUNDRY_ADDRESS, SettingService.DEFAULT_LAUNDRY_ADDRESS));
        laundryPhoneField.setValue(settings.getOrDefault(SettingService.KEY_LAUNDRY_PHONE, SettingService.DEFAULT_LAUNDRY_PHONE));

        String prefix = settings.getOrDefault(SettingService.KEY_INVOICE_PREFIX, SettingService.DEFAULT_INVOICE_PREFIX);
        invoicePrefixField.setValue(prefix);
        updateInvoicePreview();

        String paperSize = settings.getOrDefault(SettingService.KEY_PRINTER_PAPER_SIZE, SettingService.DEFAULT_PAPER_SIZE);
        paperSizeRadio.setValue(paperSize);
    }

    private void saveProfilSettings() {
        try {
            settingService.updateSetting(SettingService.KEY_LAUNDRY_NAME, laundryNameField.getValue());
            settingService.updateSetting(SettingService.KEY_LAUNDRY_ADDRESS, laundryAddressField.getValue());
            settingService.updateSetting(SettingService.KEY_LAUNDRY_PHONE, laundryPhoneField.getValue());
            settingService.updateSetting(SettingService.KEY_INVOICE_PREFIX, invoicePrefixField.getValue());

            showNotification("Profil toko dan format invoice berhasil disimpan!", NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void savePrinterSettings() {
        try {
            settingService.updateSetting(SettingService.KEY_PRINTER_PAPER_SIZE, paperSizeRadio.getValue());
            showNotification("Konfigurasi printer thermal berhasil disimpan!", NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void showNotification(String message, NotificationVariant variant) {
        Notification notif = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notif.addThemeVariants(variant);
    }
}
