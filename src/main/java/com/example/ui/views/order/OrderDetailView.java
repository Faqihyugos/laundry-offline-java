package com.example.ui.views.order;

import com.example.domain.*;
import com.example.security.AuthenticatedUser;
import com.example.service.*;
import com.example.ui.MainLayout;
import com.example.ui.components.ThermalReceiptDialog;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Route(value = "orders", layout = MainLayout.class)
@PageTitle("Detail Order | Laundry POS")
@RolesAllowed({"OWNER", "CASHIER"})
public class OrderDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final OrderService orderService;
    private final ReceiptService receiptService;
    private final SettingService settingService;
    private final AuthenticatedUser authenticatedUser;

    private Order currentOrder;

    public OrderDetailView(OrderService orderService,
                           ReceiptService receiptService,
                           SettingService settingService,
                           AuthenticatedUser authenticatedUser) {
        this.orderService = orderService;
        this.receiptService = receiptService;
        this.settingService = settingService;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
    }

    @Override
    public void setParameter(BeforeEvent event, Long orderId) {
        currentOrder = orderService.findById(orderId).orElse(null);
        removeAll();

        if (currentOrder == null) {
            add(new H2("Order tidak ditemukan."));
            Button backBtn = new Button("Kembali ke Daftar Order", e -> UI.getCurrent().navigate(OrderListView.class));
            add(backBtn);
            return;
        }

        buildUI();
    }

    private void buildUI() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        Button backBtn = new Button("Kembali ke Antrian", VaadinIcon.ARROW_LEFT.create(),
                e -> UI.getCurrent().navigate(OrderListView.class));
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        // Header section
        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setWidthFull();
        headerRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        H2 title = new H2("Order: " + currentOrder.getInvoiceNumber());
        title.getStyle().set("margin", "0").set("color", "#7B2D8E");

        Span statusBadge = new Span(currentOrder.getStatus().getDisplayName());
        statusBadge.addClassName("status-badge");
        statusBadge.addClassName(currentOrder.getStatus().getBadgeClass());

        Span payBadge = new Span(currentOrder.getPaymentStatus().getDisplayName());
        payBadge.addClassName("status-badge");
        payBadge.addClassName(currentOrder.getPaymentStatus().getBadgeClass());

        HorizontalLayout badges = new HorizontalLayout(statusBadge, payBadge);
        badges.setSpacing(true);

        headerRow.add(title, badges);

        // Visual Stepper
        HorizontalLayout stepper = createWorkflowStepper();

        // Action Toolbar
        HorizontalLayout actionToolbar = createActionToolbar();

        // Split details: Left Customer & Payment Info, Right Items
        HorizontalLayout detailsSplit = new HorizontalLayout();
        detailsSplit.setWidthFull();
        detailsSplit.setSpacing(true);

        VerticalLayout leftInfo = new VerticalLayout();
        leftInfo.setWidth("40%");
        leftInfo.setPadding(false);
        leftInfo.setSpacing(true);

        // Customer Card
        Div custCard = new Div();
        custCard.getStyle().set("background", "white")
                .set("padding", "1.25rem")
                .set("border-radius", "12px")
                .set("box-shadow", "0 1px 4px rgba(0,0,0,0.06)")
                .set("border", "1px solid #F0E6F6")
                .set("width", "100%");

        H3 custTitle = new H3("Informasi Pelanggan");
        custTitle.getStyle().set("margin", "0 0 0.5rem 0").set("font-size", "1rem").set("color", "#1A1A2E");

        Customer cust = currentOrder.getCustomer();
        Paragraph nameP = new Paragraph("👤 " + (cust != null ? cust.getName() : "-"));
        nameP.getStyle().set("margin", "0").set("font-weight", "600");

        String waUrl = cust != null ? CustomerService.buildWhatsAppUrl(cust.getPhone(),
                "Halo " + cust.getName() + ", status cucian invoice " + currentOrder.getInvoiceNumber() +
                        " saat ini: " + currentOrder.getStatus().getDisplayName()) : "#";

        Anchor waLink = new Anchor(waUrl, "Chat WhatsApp");
        waLink.setTarget("_blank");
        waLink.getStyle().set("color", "#2E7D32").set("font-size", "0.85rem");

        Anchor telLink = new Anchor(cust != null ? "tel:" + cust.getPhone() : "#", "Panggil Telepon");
        telLink.getStyle().set("color", "#1565C0").set("font-size", "0.85rem");

        HorizontalLayout commLinks = new HorizontalLayout(new Span("📱 " + (cust != null ? cust.getPhone() : "-")),
                new Span("•"), waLink, new Span("•"), telLink);
        commLinks.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        Paragraph addrP = new Paragraph("📍 " + (cust != null && cust.getAddress() != null && !cust.getAddress().isEmpty() ? cust.getAddress() : "-"));
        addrP.getStyle().set("margin", "0.25rem 0 0 0").set("color", "#6B6B7B").set("font-size", "0.85rem");

        custCard.add(custTitle, nameP, commLinks, addrP);

        // Payment Summary Card
        Div payCard = new Div();
        payCard.getStyle().set("background", "white")
                .set("padding", "1.25rem")
                .set("border-radius", "12px")
                .set("box-shadow", "0 1px 4px rgba(0,0,0,0.06)")
                .set("border", "1px solid #F0E6F6")
                .set("width", "100%");

        H3 payTitle = new H3("Ringkasan Pembayaran");
        payTitle.getStyle().set("margin", "0 0 0.5rem 0").set("font-size", "1rem").set("color", "#1A1A2E");

        Div rowTotal = createSummaryRow("Total Biaya", ReceiptService.formatRupiah(currentOrder.getTotalAmount()), true);
        Div rowPaid = createSummaryRow("Sudah Dibayar (" + currentOrder.getPaymentMethod().name() + ")", ReceiptService.formatRupiah(currentOrder.getPaidAmount()), false);
        Div rowRem = createSummaryRow("Sisa Tagihan", ReceiptService.formatRupiah(currentOrder.getRemainingAmount()), false);
        rowRem.getStyle().set("color", currentOrder.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0 ? "#C62828" : "#2E7D32");

        Paragraph datesP = new Paragraph("Tgl Masuk: " + currentOrder.getOrderDate().format(dtf) +
                "\nEstimasi Selesai: " + (currentOrder.getEstimatedCompletionDate() != null ? currentOrder.getEstimatedCompletionDate().format(dtf) : "-"));
        datesP.getStyle().set("font-size", "0.8rem").set("color", "#6B6B7B").set("white-space", "pre-line");

        payCard.add(payTitle, rowTotal, rowPaid, rowRem, new Hr(), datesP);

        leftInfo.add(custCard, payCard);

        // Right side: Items Table
        VerticalLayout rightItems = new VerticalLayout();
        rightItems.setWidth("60%");
        rightItems.setPadding(false);

        Grid<OrderItem> itemsGrid = new Grid<>(OrderItem.class, false);
        itemsGrid.addColumn(OrderItem::getServiceName).setHeader("Layanan").setAutoWidth(true);
        itemsGrid.addColumn(i -> ReceiptService.formatQuantity(i.getQuantity()) + " " + (i.getUnit() == ServiceUnit.KG ? "kg" : "pcs"))
                .setHeader("Kuantitas").setAutoWidth(true);
        itemsGrid.addColumn(i -> ReceiptService.formatRupiah(i.getUnitPrice())).setHeader("Tarif").setAutoWidth(true);
        itemsGrid.addColumn(i -> ReceiptService.formatRupiah(i.getSubtotal())).setHeader("Subtotal").setAutoWidth(true);
        itemsGrid.addColumn(OrderItem::getNotes).setHeader("Catatan").setFlexGrow(1);
        itemsGrid.setItems(currentOrder.getItems());

        rightItems.add(new H3("Rincian Cucian"), itemsGrid);

        detailsSplit.add(leftInfo, rightItems);

        add(backBtn, headerRow, stepper, actionToolbar, detailsSplit);
    }

    private HorizontalLayout createWorkflowStepper() {
        HorizontalLayout stepper = new HorizontalLayout();
        stepper.setWidthFull();
        stepper.setSpacing(true);
        stepper.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        OrderStatus[] stages = {OrderStatus.PENDING, OrderStatus.PROSES, OrderStatus.SELESAI, OrderStatus.DIAMBIL};
        OrderStatus current = currentOrder.getStatus();

        for (int i = 0; i < stages.length; i++) {
            OrderStatus stage = stages[i];
            Div stepBox = new Div();
            stepBox.addClassName("workflow-step");

            if (stage == current) {
                stepBox.addClassName("active");
                stepBox.add(VaadinIcon.DOT_CIRCLE.create(), new Span(stage.getDisplayName()));
            } else if (stage.ordinal() < current.ordinal()) {
                stepBox.addClassName("passed");
                stepBox.add(VaadinIcon.CHECK.create(), new Span(stage.getDisplayName()));
            } else {
                stepBox.add(VaadinIcon.CIRCLE_THIN.create(), new Span(stage.getDisplayName()));
            }

            stepper.add(stepBox);
            if (i < stages.length - 1) {
                Span arrow = new Span("➔");
                arrow.getStyle().set("color", "#BDBDBD");
                stepper.add(arrow);
            }
        }
        return stepper;
    }

    private HorizontalLayout createActionToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setSpacing(true);
        toolbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        OrderStatus current = currentOrder.getStatus();
        String currentUserName = authenticatedUser.get().map(User::getFullName).orElse("Staff");

        // Next Stage Action Button
        if (current == OrderStatus.PENDING) {
            Button prosesBtn = new Button("Mulai Proses Cucian", VaadinIcon.ARROW_FORWARD.create(), e -> {
                orderService.advanceStatus(currentOrder.getId(), currentUserName);
                showNotification("Status berhasil diubah ke: Proses", NotificationVariant.LUMO_SUCCESS);
                refreshView();
            });
            prosesBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            prosesBtn.getStyle().set("background-color", "#FF9800");
            toolbar.add(prosesBtn);
        } else if (current == OrderStatus.PROSES) {
            Button selesaiBtn = new Button("Selesaikan Cucian (Siap Ambil)", VaadinIcon.CHECK_CIRCLE.create(), e -> {
                orderService.advanceStatus(currentOrder.getId(), currentUserName);
                showNotification("Status berhasil diubah ke: Selesai", NotificationVariant.LUMO_SUCCESS);
                refreshView();
            });
            selesaiBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            selesaiBtn.getStyle().set("background-color", "#4CAF50");
            toolbar.add(selesaiBtn);
        } else if (current == OrderStatus.SELESAI) {
            Button diambilBtn = new Button("Tandai Diambil oleh Pelanggan", VaadinIcon.BULLSEYE.create(), e -> {
                if (currentOrder.getPaymentStatus() == PaymentStatus.LUNAS) {
                    confirmPickupDirectly();
                } else {
                    openPaymentSettlementDialog();
                }
            });
            diambilBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            diambilBtn.getStyle().set("background-color", "#7B2D8E");
            toolbar.add(diambilBtn);
        }

        // Print Struk Button
        Button printBtn = new Button("Cetak Struk", VaadinIcon.PRINT.create(), e -> {
            ThermalReceiptDialog printDialog = new ThermalReceiptDialog(receiptService, settingService, currentOrder);
            printDialog.open();
        });
        printBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        // Share WhatsApp Button
        Button waBtn = new Button("Share WhatsApp", VaadinIcon.CHAT.create(), e -> {
            try {
                String waUrl = receiptService.generateWhatsAppUrl(currentOrder);
                UI.getCurrent().getPage().open(waUrl, "_blank");
            } catch (Exception ex) {
                showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        waBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        toolbar.add(printBtn, waBtn);
        return toolbar;
    }

    private void confirmPickupDirectly() {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Konfirmasi Pengambilan Cucian");
        confirmDialog.add(new Paragraph("Pastikan pelanggan sudah memeriksa dan menerima cucian dalam kondisi bersih."));

        Button confirmBtn = new Button("Ya, Tandai Diambil", e -> {
            String currentUserName = authenticatedUser.get().map(User::getFullName).orElse("Staff");
            orderService.advanceStatus(currentOrder.getId(), currentUserName);
            showNotification("Pesanan telah selesai dan berhasil diambil!", NotificationVariant.LUMO_SUCCESS);
            confirmDialog.close();
            refreshView();
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        confirmBtn.getStyle().set("background-color", "#7B2D8E");

        Button cancelBtn = new Button("Batal", e -> confirmDialog.close());
        confirmDialog.getFooter().add(cancelBtn, confirmBtn);
        confirmDialog.open();
    }

    private void openPaymentSettlementDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Pelunasan Sisa Pembayaran");

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);

        layout.add(new Paragraph("Total Biaya: " + ReceiptService.formatRupiah(currentOrder.getTotalAmount())));
        layout.add(new Paragraph("Sudah Dibayar: " + ReceiptService.formatRupiah(currentOrder.getPaidAmount())));

        Paragraph sisaP = new Paragraph("Sisa Tagihan: " + ReceiptService.formatRupiah(currentOrder.getRemainingAmount()));
        sisaP.getStyle().set("font-weight", "bold").set("color", "#C62828");
        layout.add(sisaP);

        RadioButtonGroup<PaymentMethod> methodRadio = new RadioButtonGroup<>();
        methodRadio.setLabel("Metode Pelunasan");
        methodRadio.setItems(PaymentMethod.CASH, PaymentMethod.TRANSFER);
        methodRadio.setItemLabelGenerator(PaymentMethod::getDisplayName);
        methodRadio.setValue(PaymentMethod.CASH);

        BigDecimalField amountField = new BigDecimalField("Nominal Pelunasan (Rp)");
        amountField.setValue(currentOrder.getRemainingAmount());
        amountField.setWidthFull();

        layout.add(methodRadio, amountField);
        dialog.add(layout);

        Button settleBtn = new Button("Pelunasan & Tandai Diambil", e -> {
            try {
                String currentUserName = authenticatedUser.get().map(User::getFullName).orElse("Staff");
                orderService.settlePaymentAndAdvanceToDiambil(currentOrder.getId(), amountField.getValue(),
                        methodRadio.getValue(), currentUserName);
                showNotification("Pelunasan berhasil dicatat dan pesanan selesai diambil!", NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                refreshView();
            } catch (Exception ex) {
                showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        settleBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        settleBtn.getStyle().set("background-color", "#7B2D8E");

        Button cancelBtn = new Button("Batal", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, settleBtn);
        dialog.open();
    }

    private Div createSummaryRow(String label, String value, boolean isBold) {
        Div row = new Div();
        row.getStyle().set("display", "flex").set("justify-content", "space-between").set("margin-bottom", "4px");
        Span l = new Span(label);
        Span v = new Span(value);
        if (isBold) {
            l.getStyle().set("font-weight", "bold");
            v.getStyle().set("font-weight", "bold");
        }
        row.add(l, v);
        return row;
    }

    private void refreshView() {
        currentOrder = orderService.findById(currentOrder.getId()).orElse(null);
        removeAll();
        buildUI();
    }

    private void showNotification(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
