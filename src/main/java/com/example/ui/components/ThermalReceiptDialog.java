package com.example.ui.components;

import com.example.domain.Order;
import com.example.service.ReceiptService;
import com.example.service.SettingService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;

public class ThermalReceiptDialog extends Dialog {

    private final Pre receiptContainer = new Pre();
    private final RadioButtonGroup<String> paperSizeRadio = new RadioButtonGroup<>();
    private final ReceiptService receiptService;
    private final SettingService settingService;
    private final Order order;
    private final boolean isTestReceipt;

    public ThermalReceiptDialog(ReceiptService receiptService, SettingService settingService, Order order) {
        this(receiptService, settingService, order, false);
    }

    public ThermalReceiptDialog(ReceiptService receiptService, SettingService settingService, Order order, boolean isTestReceipt) {
        this.receiptService = receiptService;
        this.settingService = settingService;
        this.order = order;
        this.isTestReceipt = isTestReceipt;

        setHeaderTitle(isTestReceipt ? "Cetak Struk Uji Coba Printer" : "Cetak Struk Transaksi Laundry");
        setWidth("480px");

        buildUI();
    }

    private void buildUI() {
        String defaultPaper = settingService.getSetting(SettingService.KEY_PRINTER_PAPER_SIZE, SettingService.DEFAULT_PRINTER_PAPER_SIZE);

        paperSizeRadio.setLabel("Ukuran Kertas Thermal");
        paperSizeRadio.setItems("58mm", "80mm");
        paperSizeRadio.setValue(defaultPaper);
        paperSizeRadio.addValueChangeListener(e -> updateReceiptText(e.getValue()));

        receiptContainer.addClassName("thermal-receipt-preview");
        updateReceiptText(paperSizeRadio.getValue());

        VerticalLayout layout = new VerticalLayout(paperSizeRadio, receiptContainer);
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);
        add(layout);

        Button printBtn = new Button("Cetak", VaadinIcon.PRINT.create(), e -> {
            UI.getCurrent().getPage().executeJs("window.print()");
        });
        printBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        printBtn.getStyle().set("background", "#3949ab")
                .set("box-shadow", "0 4px 12px rgba(57, 73, 171, 0.25)")
                .set("border-radius", "8px")
                .set("font-family", "'Barlow', sans-serif");

        Button closeBtn = new Button("Tutup", e -> close());
        getFooter().add(closeBtn, printBtn);
    }

    private void updateReceiptText(String paperSize) {
        receiptContainer.removeClassName("receipt-58mm");
        receiptContainer.removeClassName("receipt-80mm");

        if ("80mm".equalsIgnoreCase(paperSize)) {
            receiptContainer.addClassName("receipt-80mm");
        } else {
            receiptContainer.addClassName("receipt-58mm");
        }

        if (isTestReceipt || order == null) {
            receiptContainer.setText(settingService.generateTestReceipt(paperSize));
        } else {
            receiptContainer.setText(receiptService.generateThermalReceipt(order, paperSize));
        }
    }
}
