package com.example.ui.views.setting;

import com.example.service.ReceiptService;
import com.example.service.SettingService;
import com.example.ui.MainLayout;
import com.example.ui.components.ThermalReceiptDialog;
import com.vaadin.flow.component.UI;
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

    // Sub-Navigation Tabs matching Figma node 14:2927 exactly
    private enum SettingTab {
        PRODUK("Kelola Produk", "Kelola Produk, Harga, Ketersediaan, dll.", VaadinIcon.GRID_SMALL),
        DISKON("Kelola Diskon", "Kelola Diskon Pelanggan", VaadinIcon.TAG),
        PRINTER("Kelola Printer", "Tambah atau hapus printer", VaadinIcon.PRINT),
        PAJAK("Kelola Pajak", "Kelola pajak 10 atau 11 %", VaadinIcon.INVOICE),
        PROFIL("Profil Toko", "Identitas outlet & prefix invoice", VaadinIcon.SHOP);

        final String label;
        final String subtitle;
        final VaadinIcon icon;

        SettingTab(String label, String subtitle, VaadinIcon icon) {
            this.label = label;
            this.subtitle = subtitle;
            this.icon = icon;
        }
    }

    private SettingTab currentTab = SettingTab.DISKON;

    private final Div navContainer = new Div();
    private final Div contentArea = new Div();

    // Tab: Profil Toko Fields
    private final TextField laundryNameField = new TextField("Nama Laundry");
    private final TextArea laundryAddressField = new TextArea("Alamat Lengkap");
    private final TextField laundryPhoneField = new TextField("Nomor Telepon / WhatsApp");
    private final TextField invoicePrefixField = new TextField("Prefix Nomor Invoice");
    private final Span invoicePreview = new Span();

    // Tab: Diskon State
    private record DiscountItem(String name, String desc, String type, String value, String category) {}
    private final List<DiscountItem> customDiscounts = new ArrayList<>();
    private String selectedDiskonCategory = "Semua";

    // Tab: Printer Fields
    private final TextField printerNameField = new TextField("Nama Printer");
    private final ComboBox<String> printerConnectionType = new ComboBox<>("Tipe Koneksi Printer");
    private final RadioButtonGroup<String> paperSizeRadio = new RadioButtonGroup<>();
    private final Checkbox autoCutCheckbox = new Checkbox("Potong kertas otomatis setelah cetak");
    private final Checkbox autoPrintCheckbox = new Checkbox("Cetak struk otomatis setelah pembayaran");
    private String selectedPrinterCategory = "Wifi / LAN";

    // Tab: Pajak Fields
    private final TextField taxNameField = new TextField("Nama Pajak");
    private final RadioButtonGroup<String> taxRateRadio = new RadioButtonGroup<>();
    private final Checkbox taxActiveCheckbox = new Checkbox("Aktifkan perhitungan pajak pada pesanan baru");
    private String selectedPajakCategory = "Semua";

    public SettingView(SettingService settingService, ReceiptService receiptService) {
        this.settingService = settingService;
        this.receiptService = receiptService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("padding", "24px 32px").set("background-color", "#FAFAFA");

        initDefaultDiscounts();
        createHeader();
        createTwoColumnLayout();
        loadSettings();
    }

    private void initDefaultDiscounts() {
        customDiscounts.add(new DiscountItem("Diskon Pelanggan Member", "Potongan tarif 10% untuk member setia laundry", "Persentase", "10%", "Semua"));
        customDiscounts.add(new DiscountItem("Promo Spesial Cuci Kiloan", "Promo potongan 15% menyambut pembukaan outlet", "Persentase", "15%", "Kiloan"));
        customDiscounts.add(new DiscountItem("Potongan Tunai Langsung", "Potongan langsung Rp 5.000 untuk transaksi non-promo", "Nominal", "Rp 5.000", "Satuan"));
    }

    private void createHeader() {
        H2 title = new H2("Settings");
        title.getStyle().set("margin", "0 0 20px 0")
                .set("color", "#3949ab")
                .set("font-size", "28px")
                .set("font-weight", "600")
                .set("font-family", "'Barlow', sans-serif");

        add(title);
    }

    private void createTwoColumnLayout() {
        Div container = new Div();
        container.addClassName("settings-container");

        // 1. Left: Sub-Navigation Panel (275px, Figma 14:2935)
        navContainer.addClassName("settings-sidebar-nav");
        renderNavItems();

        // 2. Right: Content Area (743px, Figma 14:2937)
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

            Div iconBox = new Div();
            iconBox.addClassName("settings-nav-item-icon");
            Icon icon = tab.icon.create();
            icon.setSize("20px");
            iconBox.add(icon);

            Div textContent = new Div();
            textContent.addClassName("settings-nav-item-content");

            Span titleSpan = new Span(tab.label);
            titleSpan.addClassName("settings-nav-item-title");

            Span subSpan = new Span(tab.subtitle);
            subSpan.addClassName("settings-nav-item-sub");

            textContent.add(titleSpan, subSpan);
            item.add(iconBox, textContent);

            item.addClickListener(e -> {
                if (tab == SettingTab.PRODUK) {
                    UI.getCurrent().navigate("services");
                    return;
                }
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
            case DISKON -> renderDiskonTab();
            case PRINTER -> renderPrinterTab();
            case PAJAK -> renderPajakTab();
            case PROFIL -> renderProfilTab();
            case PRODUK -> {}
        }
    }

    // ==========================================
    // TAB 1: KELOLA DISKON (Figma Node 14:2927)
    // ==========================================
    private void renderDiskonTab() {
        Div header = new Div();
        header.addClassName("settings-content-header");

        H3 title = new H3("Kelola Diskon");
        title.addClassName("settings-content-title");

        Div tabsRow = new Div();
        tabsRow.addClassName("settings-tabs-row");

        String[] categories = {"Semua", "Kiloan", "Satuan", "Layanan Lain"};
        for (String cat : categories) {
            Button catBtn = new Button(cat);
            catBtn.addClassName("settings-tab-btn");
            if (cat.equals(selectedDiskonCategory)) {
                catBtn.addClassName("active");
            }
            catBtn.addClickListener(e -> {
                selectedDiskonCategory = cat;
                renderDiskonTab();
            });
            tabsRow.add(catBtn);
        }

        header.add(title, tabsRow);

        // Grid of Discount Cards
        Div grid = new Div();
        grid.addClassName("setting-card-grid");

        // 1. First Card: + Tambah Diskon Baru (dashed, Figma node 14:2954)
        Div addCard = new Div();
        addCard.addClassName("setting-action-card");
        Div plusBox = new Div();
        plusBox.addClassName("plus-box");
        Icon plusIcon = VaadinIcon.PLUS.create();
        plusIcon.setSize("20px");
        plusBox.add(plusIcon);
        Span addLabel = new Span("Tambah Diskon Baru");
        addCard.add(plusBox, addLabel);
        addCard.addClickListener(e -> openAddDiscountDialog());
        grid.add(addCard);

        // 2. Discount Cards matching Figma node 14:2959
        for (DiscountItem item : customDiscounts) {
            if (!"Semua".equals(selectedDiskonCategory) && !item.category().equalsIgnoreCase(selectedDiskonCategory) && !"Semua".equalsIgnoreCase(item.category())) {
                continue;
            }

            Div card = new Div();
            card.addClassName("figma-discount-card");

            // Top-right edit button
            Button editBtn = new Button(VaadinIcon.EDIT.create());
            editBtn.addClassName("card-edit-btn");
            editBtn.addClickListener(e -> openEditDiscountDialog(item));
            card.add(editBtn);

            // Center circular icon with concentric rings
            Div centerCircle = new Div();
            centerCircle.addClassName("card-circle-center");
            Icon tagIcon = VaadinIcon.TAG.create();
            tagIcon.setSize("36px");
            centerCircle.add(tagIcon);
            card.add(centerCircle);

            // Bottom text
            Div bottomContent = new Div();
            bottomContent.getStyle().set("display", "flex").set("flex-direction", "column");

            Span itemTitle = new Span(item.name());
            itemTitle.addClassName("card-title");

            Span itemCat = new Span(item.category());
            itemCat.addClassName("card-cat");

            Span itemPrice = new Span(item.value());
            itemPrice.addClassName("card-price");

            bottomContent.add(itemTitle, itemCat, itemPrice);
            card.add(bottomContent);

            grid.add(card);
        }

        // Footer buttons (Batalkan & Simpan)
        Div footer = createFooterActions(() -> renderDiskonTab(), () -> {
            showNotification("Perubahan diskon berhasil disimpan!", NotificationVariant.LUMO_SUCCESS);
        });

        contentArea.add(header, grid, footer);
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

        ComboBox<String> catBox = new ComboBox<>("Kategori Layanan");
        catBox.setItems("Semua", "Kiloan", "Satuan", "Layanan Lain");
        catBox.setValue("Semua");
        catBox.setWidthFull();

        RadioButtonGroup<String> typeRadio = new RadioButtonGroup<>();
        typeRadio.setLabel("Tipe Diskon");
        typeRadio.setItems("Persentase (%)", "Nominal (Rp)");
        typeRadio.setValue("Persentase (%)");

        BigDecimalField valField = new BigDecimalField("Besaran Nilai");
        valField.setWidthFull();
        valField.setPlaceholder("Contoh: 10 atau 5000");

        VerticalLayout form = new VerticalLayout(nameField, descField, catBox, typeRadio, valField);
        form.setPadding(false);

        Button saveBtn = new Button("Simpan Diskon", e -> {
            if (nameField.getValue() == null || nameField.getValue().trim().isEmpty() || valField.getValue() == null) {
                showNotification("Lengkapi nama diskon dan nilai diskon", NotificationVariant.LUMO_WARNING);
                return;
            }

            boolean isPct = "Persentase (%)".equals(typeRadio.getValue());
            String displayVal = isPct ? valField.getValue() + "%" : ReceiptService.formatRupiah(valField.getValue());
            customDiscounts.add(new DiscountItem(nameField.getValue(), descField.getValue(), isPct ? "Persentase" : "Nominal", displayVal, catBox.getValue()));
            renderDiskonTab();
            showNotification("Diskon berhasil ditambahkan!", NotificationVariant.LUMO_SUCCESS);
            dialog.close();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.getStyle().set("background", "#3949ab");

        Button cancelBtn = new Button("Batal", e -> dialog.close());
        dialog.add(form);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void openEditDiscountDialog(DiscountItem item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Diskon");
        dialog.setWidth("440px");

        TextField nameField = new TextField("Nama Diskon", item.name());
        nameField.setWidthFull();

        TextArea descField = new TextArea("Deskripsi Promo", item.desc());
        descField.setWidthFull();

        Button deleteBtn = new Button("Hapus Diskon", VaadinIcon.TRASH.create(), e -> {
            customDiscounts.remove(item);
            renderDiskonTab();
            showNotification("Diskon berhasil dihapus", NotificationVariant.LUMO_SUCCESS);
            dialog.close();
        });
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Button saveBtn = new Button("Simpan", e -> {
            customDiscounts.remove(item);
            customDiscounts.add(new DiscountItem(nameField.getValue(), descField.getValue(), item.type(), item.value(), item.category()));
            renderDiskonTab();
            showNotification("Perubahan diskon berhasil disimpan", NotificationVariant.LUMO_SUCCESS);
            dialog.close();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.getStyle().set("background", "#3949ab");

        Button cancelBtn = new Button("Batal", e -> dialog.close());

        VerticalLayout layout = new VerticalLayout(nameField, descField);
        layout.setPadding(false);
        dialog.add(layout);
        dialog.getFooter().add(deleteBtn, cancelBtn, saveBtn);
        dialog.open();
    }

    // ==========================================
    // TAB 2: KELOLA PRINTER (Figma Node 14:3521)
    // ==========================================
    private void renderPrinterTab() {
        Div header = new Div();
        header.addClassName("settings-content-header");

        H3 title = new H3("Kelola Printer");
        title.addClassName("settings-content-title");

        Div tabsRow = new Div();
        tabsRow.addClassName("settings-tabs-row");

        String[] categories = {"Wifi / LAN", "Bluetooth"};
        for (String cat : categories) {
            Button catBtn = new Button(cat);
            catBtn.addClassName("settings-tab-btn");
            if (cat.equals(selectedPrinterCategory)) {
                catBtn.addClassName("active");
            }
            catBtn.addClickListener(e -> {
                selectedPrinterCategory = cat;
                renderPrinterTab();
            });
            tabsRow.add(catBtn);
        }

        header.add(title, tabsRow);

        Div grid = new Div();
        grid.addClassName("setting-card-grid");

        // Action Card: + Tambah Printer
        Div addCard = new Div();
        addCard.addClassName("setting-action-card");
        Div plusBox = new Div();
        plusBox.addClassName("plus-box");
        Icon plusIcon = VaadinIcon.PLUS.create();
        plusIcon.setSize("20px");
        plusBox.add(plusIcon);
        Span addLabel = new Span("Tambah Printer");
        addCard.add(plusBox, addLabel);
        addCard.addClickListener(e -> showNotification("Pencarian printer " + selectedPrinterCategory + " sedang berlangsung...", NotificationVariant.LUMO_PRIMARY));
        grid.add(addCard);

        // Printer Config Card
        Div printerCard = new Div();
        printerCard.addClassName("figma-discount-card");
        printerCard.getStyle().set("min-width", "360px").set("flex", "1");

        H3 cardHeader = new H3("Konfigurasi Printer Utama");
        cardHeader.getStyle().set("margin", "0 0 12px 0").set("color", "#3949ab").set("font-size", "16px").set("font-weight", "600");

        printerNameField.setWidthFull();
        printerNameField.setValue("Thermal Receipt Printer POS");

        printerConnectionType.setWidthFull();
        printerConnectionType.setItems("USB Direct Print", "Bluetooth Portable", "Network / Ethernet LAN");
        printerConnectionType.setValue("Network / Ethernet LAN".contains(selectedPrinterCategory) ? "Network / Ethernet LAN" : "Bluetooth Portable");

        paperSizeRadio.setLabel("Ukuran Kertas Struk");
        paperSizeRadio.setItems("58mm", "80mm");

        autoCutCheckbox.setValue(true);
        autoPrintCheckbox.setValue(false);

        VerticalLayout toggles = new VerticalLayout(autoCutCheckbox, autoPrintCheckbox);
        toggles.setPadding(false);
        toggles.setSpacing(false);

        Button testPrintBtn = new Button("Uji Coba Cetak Struk (Test Print)", VaadinIcon.PRINT.create(), e -> {
            ThermalReceiptDialog testDialog = new ThermalReceiptDialog(receiptService, settingService, null, true);
            testDialog.open();
        });
        testPrintBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        testPrintBtn.getStyle().set("margin-top", "8px");

        VerticalLayout configLayout = new VerticalLayout(cardHeader, printerNameField, printerConnectionType, paperSizeRadio, toggles, testPrintBtn);
        configLayout.setPadding(false);
        configLayout.setSpacing(true);
        printerCard.add(configLayout);

        grid.add(printerCard);

        Div footer = createFooterActions(() -> renderPrinterTab(), () -> savePrinterSettings());
        contentArea.add(header, grid, footer);
    }

    // ==========================================
    // TAB 3: KELOLA PAJAK (Figma Node 14:3722)
    // ==========================================
    private void renderPajakTab() {
        Div header = new Div();
        header.addClassName("settings-content-header");

        H3 title = new H3("Kelola Pajak");
        title.addClassName("settings-content-title");

        Div tabsRow = new Div();
        tabsRow.addClassName("settings-tabs-row");

        String[] categories = {"Semua", "Pajak PPN"};
        for (String cat : categories) {
            Button catBtn = new Button(cat);
            catBtn.addClassName("settings-tab-btn");
            if (cat.equals(selectedPajakCategory)) {
                catBtn.addClassName("active");
            }
            catBtn.addClickListener(e -> {
                selectedPajakCategory = cat;
                renderPajakTab();
            });
            tabsRow.add(catBtn);
        }

        header.add(title, tabsRow);

        Div grid = new Div();
        grid.addClassName("setting-card-grid");

        // Action Card: + Tambah Pajak
        Div addCard = new Div();
        addCard.addClassName("setting-action-card");
        Div plusBox = new Div();
        plusBox.addClassName("plus-box");
        Icon plusIcon = VaadinIcon.PLUS.create();
        plusIcon.setSize("20px");
        plusBox.add(plusIcon);
        Span addLabel = new Span("Tambah Pajak");
        addCard.add(plusBox, addLabel);
        addCard.addClickListener(e -> showNotification("Fitur penambahan skema pajak kustom siap digunakan", NotificationVariant.LUMO_PRIMARY));
        grid.add(addCard);

        // Tax Config Card
        Div taxCard = new Div();
        taxCard.addClassName("figma-discount-card");
        taxCard.getStyle().set("min-width", "360px").set("flex", "1");

        H3 taxHeader = new H3("Pengaturan Pajak Transaksi & PPN");
        taxHeader.getStyle().set("margin", "0 0 12px 0").set("color", "#3949ab").set("font-size", "16px").set("font-weight", "600");

        taxNameField.setWidthFull();
        taxNameField.setValue("Pajak Pertambahan Nilai (PPN)");

        taxRateRadio.setLabel("Tarif Pajak Standar");
        taxRateRadio.setItems("11% (PPN Standar Nasional)", "0% (Bebas Pajak)");
        taxRateRadio.setValue("11% (PPN Standar Nasional)");

        taxActiveCheckbox.setValue(false);

        Paragraph info = new Paragraph("Pengaturan ini menentukan pilihan pajak default saat kasir membuat tiket pesanan baru di POS.");
        info.getStyle().set("color", "#7890cd").set("font-size", "12px");

        VerticalLayout taxLayout = new VerticalLayout(taxHeader, taxNameField, taxRateRadio, taxActiveCheckbox, info);
        taxLayout.setPadding(false);
        taxLayout.setSpacing(true);
        taxCard.add(taxLayout);

        grid.add(taxCard);

        Div footer = createFooterActions(() -> renderPajakTab(), () -> {
            showNotification("Pengaturan pajak berhasil disimpan!", NotificationVariant.LUMO_SUCCESS);
        });
        contentArea.add(header, grid, footer);
    }

    // ==========================================
    // TAB 4: PROFIL TOKO
    // ==========================================
    private void renderProfilTab() {
        Div header = new Div();
        header.addClassName("settings-content-header");

        H3 title = new H3("Profil Toko & Format Invoice");
        title.addClassName("settings-content-title");

        Div tabsRow = new Div();
        tabsRow.addClassName("settings-tabs-row");

        Button b1 = new Button("Profil Laundry");
        b1.addClassName("settings-tab-btn");
        b1.addClassName("active");
        tabsRow.add(b1);

        header.add(title, tabsRow);

        Div grid = new Div();
        grid.addClassName("setting-card-grid");

        Div formCard = new Div();
        formCard.addClassName("figma-discount-card");
        formCard.getStyle().set("min-width", "420px").set("flex", "1");

        laundryNameField.setWidthFull();
        laundryNameField.setRequired(true);

        laundryAddressField.setWidthFull();
        laundryAddressField.setPlaceholder("Alamat lengkap outlet laundry...");

        laundryPhoneField.setWidthFull();
        laundryPhoneField.setRequired(true);
        laundryPhoneField.setPlaceholder("Nomor telepon struk & WhatsApp...");

        invoicePrefixField.setWidthFull();
        invoicePrefixField.setRequired(true);
        invoicePrefixField.setHelperText("Prefix 2-6 karakter alfanumerik huruf kapital tanpa spasi (contoh: LBW, INV)");
        invoicePrefixField.addValueChangeListener(e -> updateInvoicePreview());

        HorizontalLayout previewLayout = new HorizontalLayout(new Span("Contoh Nomor Invoice: "), invoicePreview);
        previewLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        invoicePreview.getStyle().set("font-weight", "800").set("color", "#3949ab");

        VerticalLayout vForm = new VerticalLayout(laundryNameField, laundryAddressField, laundryPhoneField, invoicePrefixField, previewLayout);
        vForm.setPadding(false);
        vForm.setSpacing(true);
        formCard.add(vForm);

        grid.add(formCard);

        Div footer = createFooterActions(() -> loadSettings(), () -> saveProfilSettings());
        contentArea.add(header, grid, footer);
    }

    private Div createFooterActions(Runnable onCancel, Runnable onSave) {
        Div footer = new Div();
        footer.addClassName("settings-footer-actions");

        Button cancelBtn = new Button("Batalkan", e -> onCancel.run());
        cancelBtn.addClassName("settings-btn-cancel");

        Button saveBtn = new Button("Simpan", e -> onSave.run());
        saveBtn.addClassName("settings-btn-save");

        footer.add(cancelBtn, saveBtn);
        return footer;
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

        String paperSize = settings.getOrDefault(SettingService.KEY_PRINTER_PAPER_SIZE, SettingService.DEFAULT_PRINTER_PAPER_SIZE);
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
