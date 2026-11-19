package com.example.ui.views.setting;

import com.example.service.ReceiptService;
import com.example.service.SettingService;
import com.example.ui.MainLayout;
import com.example.ui.components.ThermalReceiptDialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Pengaturan | Laundry POS")
@RolesAllowed("OWNER")
public class SettingView extends VerticalLayout {

    private final SettingService settingService;
    private final ReceiptService receiptService;

    private final TextField laundryNameField = new TextField("Nama Laundry");
    private final TextArea laundryAddressField = new TextArea("Alamat Lengkap");
    private final TextField laundryPhoneField = new TextField("Nomor Telepon / WhatsApp");

    private final TextField invoicePrefixField = new TextField("Prefix Nomor Invoice");
    private final Span invoicePreview = new Span();

    private final RadioButtonGroup<String> paperSizeRadio = new RadioButtonGroup<>();

    public SettingView(SettingService settingService, ReceiptService receiptService) {
        this.settingService = settingService;
        this.receiptService = receiptService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("max-width", "1280px").set("margin", "0 auto");

        createHeader();
        createCards();
        loadSettings();
    }

    private void createHeader() {
        H2 title = new H2("Pengaturan Toko & Printer");
        title.getStyle().set("margin", "0")
                .set("color", "var(--text-main)")
                .set("font-size", "1.6rem")
                .set("font-weight", "800")
                .set("letter-spacing", "-0.02em");

        Paragraph subtitle = new Paragraph("Konfigurasikan informasi laundry, format invoice, dan ukuran kertas thermal printer.");
        subtitle.getStyle().set("margin", "4px 0 0.5rem 0").set("color", "var(--text-muted)").set("font-size", "0.88rem");

        add(title, subtitle);
    }

    private void createCards() {
        VerticalLayout container = new VerticalLayout();
        container.setMaxWidth("760px");
        container.setPadding(false);
        container.setSpacing(true);

        // Card 1: Shop Info
        Div shopCard = createCardWrapper("1. Profil Laundry");
        laundryNameField.setWidthFull();
        laundryNameField.setRequired(true);

        laundryAddressField.setWidthFull();
        laundryAddressField.setPlaceholder("Alamat outlet laundry");

        laundryPhoneField.setWidthFull();
        laundryPhoneField.setRequired(true);
        laundryPhoneField.setPlaceholder("Nomor telepon yang tampil di struk dan link WA");

        shopCard.add(laundryNameField, laundryAddressField, laundryPhoneField);

        // Card 2: Invoice Format
        Div invoiceCard = createCardWrapper("2. Format Nomor Invoice");
        invoicePrefixField.setWidthFull();
        invoicePrefixField.setRequired(true);
        invoicePrefixField.setHelperText("Prefix 2-6 karakter alfanumerik huruf kapital tanpa spasi (contoh: LBW, INV)");
        invoicePrefixField.addValueChangeListener(e -> updateInvoicePreview());

        HorizontalLayout previewLayout = new HorizontalLayout(new Span("Contoh Nomor Invoice: "), invoicePreview);
        previewLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        invoicePreview.getStyle().set("font-weight", "800").set("color", "var(--primary)");

        invoiceCard.add(invoicePrefixField, previewLayout);

        // Card 3: Printer Configuration
        Div printerCard = createCardWrapper("3. Konfigurasi Printer Struk Thermal");
        paperSizeRadio.setLabel("Ukuran Lebar Kertas Struk");
        paperSizeRadio.setItems("58mm", "80mm");

        Button testPrintBtn = new Button("Cetak Struk Uji Coba", VaadinIcon.PRINT.create(), e -> {
            ThermalReceiptDialog testDialog = new ThermalReceiptDialog(receiptService, settingService, null, true);
            testDialog.open();
        });
        testPrintBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        printerCard.add(paperSizeRadio, testPrintBtn);

        // Action Buttons
        Button saveBtn = new Button("Simpan Pengaturan", VaadinIcon.CHECK.create(), e -> saveSettings());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        saveBtn.getStyle().set("background", "var(--primary-gradient)")
                .set("box-shadow", "0 4px 12px rgba(79, 70, 229, 0.25)")
                .set("border-radius", "10px");

        Button resetBtn = new Button("Kembalikan ke Default", VaadinIcon.REFRESH.create(), e -> confirmReset());
        resetBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout actions = new HorizontalLayout(saveBtn, resetBtn);
        actions.setSpacing(true);

        container.add(shopCard, invoiceCard, printerCard, actions);
        add(container);
    }

    private Div createCardWrapper(String headerText) {
        Div card = new Div();
        card.addClassName("modern-card");

        H3 title = new H3(headerText);
        title.getStyle().set("margin", "0 0 1rem 0").set("color", "#191426").set("font-size", "1.1rem").set("font-weight", "700");
        card.add(title);
        return card;
    }

    private void loadSettings() {
        Map<String, String> settings = settingService.getAllSettings();
        laundryNameField.setValue(settings.get(SettingService.KEY_LAUNDRY_NAME));
        laundryAddressField.setValue(settings.get(SettingService.KEY_LAUNDRY_ADDRESS));
        laundryPhoneField.setValue(settings.get(SettingService.KEY_LAUNDRY_PHONE));
        invoicePrefixField.setValue(settings.get(SettingService.KEY_INVOICE_PREFIX));
        paperSizeRadio.setValue(settings.get(SettingService.KEY_PRINTER_PAPER_SIZE));
        updateInvoicePreview();
    }

    private void updateInvoicePreview() {
        String pfx = invoicePrefixField.getValue();
        if (pfx == null || pfx.trim().isEmpty()) {
            pfx = "LBW";
        }
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        invoicePreview.setText(pfx.trim().toUpperCase() + "-" + dateStr + "-0001");
    }

    private void saveSettings() {
        try {
            settingService.saveAllSettings(
                    laundryNameField.getValue(),
                    laundryAddressField.getValue(),
                    laundryPhoneField.getValue(),
                    invoicePrefixField.getValue(),
                    paperSizeRadio.getValue()
            );
            showNotification("Pengaturan berhasil disimpan.", NotificationVariant.LUMO_SUCCESS);
            loadSettings();
        } catch (Exception ex) {
            showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void confirmReset() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Kembalikan Pengaturan ke Default");
        dialog.add(new Paragraph("Apakah Anda yakin ingin mengembalikan seluruh pengaturan ke konfigurasi bawaan?"));

        Button resetBtn = new Button("Ya, Kembalikan", e -> {
            settingService.resetToDefaults();
            showNotification("Pengaturan berhasil dikembalikan ke default.", NotificationVariant.LUMO_SUCCESS);
            dialog.close();
            loadSettings();
        });
        resetBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button cancelBtn = new Button("Batal", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, resetBtn);
        dialog.open();
    }

    private void showNotification(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
