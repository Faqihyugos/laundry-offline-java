package com.example.ui.views.order;

import com.example.domain.*;
import com.example.security.AuthenticatedUser;
import com.example.service.*;
import com.example.ui.MainLayout;
import com.example.ui.components.ThermalReceiptDialog;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Route(value = "orders/new", layout = MainLayout.class)
@PageTitle("Order Baru | Laundry POS")
@RolesAllowed({"OWNER", "CASHIER"})
public class CreateOrderView extends VerticalLayout {

    private final OrderService orderService;
    private final CustomerService customerService;
    private final ServicePackageService servicePackageService;
    private final ReceiptService receiptService;
    private final SettingService settingService;
    private final AuthenticatedUser authenticatedUser;

    // Service catalog (Left panel)
    private final TextField searchServiceField = new TextField();
    private final Grid<LaundryService> serviceGrid = new Grid<>(LaundryService.class, false);

    // Cart items (Right panel)
    private final List<OrderItem> cartItems = new ArrayList<>();
    private final VerticalLayout cartItemsContainer = new VerticalLayout();
    private final ComboBox<Customer> customerComboBox = new ComboBox<>("Pilih Pelanggan");
    private final DateTimePicker estDatePicker = new DateTimePicker("Estimasi Selesai");
    private final RadioButtonGroup<PaymentMethod> paymentMethodRadio = new RadioButtonGroup<>();
    private final BigDecimalField paidAmountField = new BigDecimalField("Nominal Bayar (Rp)");
    private final TextArea generalNotesField = new TextArea("Catatan Pesanan");

    private final Span totalAmountSpan = new Span("Rp 0");
    private final Span paymentStatusBadge = new Span("Belum Lunas");
    private final Span balanceInfoSpan = new Span("");

    public CreateOrderView(OrderService orderService,
                           CustomerService customerService,
                           ServicePackageService servicePackageService,
                           ReceiptService receiptService,
                           SettingService settingService,
                           AuthenticatedUser authenticatedUser) {
        this.orderService = orderService;
        this.customerService = customerService;
        this.servicePackageService = servicePackageService;
        this.receiptService = receiptService;
        this.settingService = settingService;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("max-width", "1280px").set("margin", "0 auto");

        createHeader();
        createSplitLayout();
    }

    private void createHeader() {
        H2 title = new H2("Kasir - Buat Order Baru");
        title.getStyle().set("margin", "0").set("color", "#7B2D8E").set("font-weight", "800");

        Paragraph subtitle = new Paragraph("Input pesanan cucian, tentukan estimasi selesai, dan catat status pembayaran.");
        subtitle.getStyle().set("margin", "4px 0 0.5rem 0").set("color", "#6C667A").set("font-size", "0.9rem");

        add(title, subtitle);
    }

    private void createSplitLayout() {
        Div split = new Div();
        split.addClassName("responsive-split");

        Div leftPanel = createCatalogPanel();
        leftPanel.addClassName("responsive-split-col");

        Div rightPanel = createCartPanel();
        rightPanel.addClassName("responsive-split-col");

        split.add(leftPanel, rightPanel);
        add(split);
    }

    private Div createCatalogPanel() {
        Div panel = new Div();
        panel.addClassName("modern-card");

        H3 title = new H3("1. Pilih Paket Layanan");
        title.getStyle().set("margin", "0 0 0.75rem 0").set("color", "#191426").set("font-size", "1.1rem").set("font-weight", "700");

        searchServiceField.setPlaceholder("Cari layanan cucian...");
        searchServiceField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchServiceField.setClearButtonVisible(true);
        searchServiceField.setWidthFull();
        searchServiceField.setValueChangeMode(ValueChangeMode.LAZY);
        searchServiceField.addValueChangeListener(e -> refreshServiceGrid());

        serviceGrid.setHeight("420px");
        serviceGrid.addColumn(LaundryService::getName).setHeader("Layanan").setAutoWidth(true);
        serviceGrid.addColumn(s -> ReceiptService.formatRupiah(s.getPrice()) + " / " + s.getUnit().name().toLowerCase())
                .setHeader("Tarif").setAutoWidth(true);
        serviceGrid.addColumn(s -> s.getEstimatedDurationHours() + " Jam").setHeader("Durasi").setAutoWidth(true);

        serviceGrid.addComponentColumn(service -> {
            Button addBtn = new Button("Tambah", VaadinIcon.PLUS.create(), e -> addItemToCart(service));
            addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            addBtn.getStyle().set("background-color", "#7B2D8E");
            return addBtn;
        }).setHeader("Aksi").setAutoWidth(true);

        panel.add(title, searchServiceField, serviceGrid);
        refreshServiceGrid();
        return panel;
    }

    private Div createCartPanel() {
        Div panel = new Div();
        panel.addClassName("modern-card");

        H3 title = new H3("2. Rincian Tiket Order");
        title.getStyle().set("margin", "0 0 0.75rem 0").set("color", "#7B2D8E").set("font-size", "1.1rem").set("font-weight", "700");

        // Customer selection row with quick add
        customerComboBox.setWidthFull();
        customerComboBox.setPlaceholder("Pilih atau cari pelanggan...");
        customerComboBox.setItemLabelGenerator(c -> c.getName() + " (" + c.getPhone() + ")");
        refreshCustomerCombo();

        Button quickAddCustomerBtn = new Button(VaadinIcon.USER_CHECK.create(), e -> openInlineCustomerModal());
        quickAddCustomerBtn.setTooltipText("Tambah Pelanggan Baru");
        quickAddCustomerBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        quickAddCustomerBtn.getStyle().set("background-color", "#7B2D8E");

        HorizontalLayout customerRow = new HorizontalLayout(customerComboBox, quickAddCustomerBtn);
        customerRow.setWidthFull();
        customerRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);

        // Cart items container
        cartItemsContainer.setWidthFull();
        cartItemsContainer.setPadding(false);
        cartItemsContainer.setSpacing(true);

        // Estimated Completion Date
        estDatePicker.setWidthFull();
        estDatePicker.setValue(LocalDateTime.now().plusHours(24));

        // Payment section
        paymentMethodRadio.setLabel("Metode Pembayaran");
        paymentMethodRadio.setItems(PaymentMethod.CASH, PaymentMethod.TRANSFER);
        paymentMethodRadio.setItemLabelGenerator(PaymentMethod::getDisplayName);
        paymentMethodRadio.setValue(PaymentMethod.CASH);

        paidAmountField.setWidthFull();
        paidAmountField.setValue(BigDecimal.ZERO);
        paidAmountField.setValueChangeMode(ValueChangeMode.EAGER);
        paidAmountField.addValueChangeListener(e -> recalculateCartTotals());

        paymentStatusBadge.addClassName("status-badge");
        paymentStatusBadge.addClassName("badge-belum-lunas");

        HorizontalLayout totalRow = new HorizontalLayout(
                new Span("TOTAL: "), totalAmountSpan, new Span("•"), paymentStatusBadge);
        totalRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        totalRow.setSpacing(true);
        totalAmountSpan.getStyle().set("font-size", "1.4rem").set("font-weight", "800").set("color", "#7B2D8E");

        balanceInfoSpan.getStyle().set("font-size", "0.9rem").set("font-weight", "600");

        generalNotesField.setWidthFull();
        generalNotesField.setPlaceholder("Catatan khusus / preferensi aroma...");

        Button submitBtn = new Button("Proses & Simpan Order", VaadinIcon.CHECK_CIRCLE.create(), e -> submitOrder());
        submitBtn.setWidthFull();
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        submitBtn.getStyle().set("background-color", "#7B2D8E").set("margin-top", "1rem");

        panel.add(title, customerRow, new Hr(), new Span("Item Layanan Cucian:"),
                cartItemsContainer, new Hr(), estDatePicker, paymentMethodRadio,
                paidAmountField, totalRow, balanceInfoSpan, generalNotesField, submitBtn);

        renderCartItems();
        return panel;
    }

    private void refreshServiceGrid() {
        serviceGrid.setItems(servicePackageService.search(searchServiceField.getValue())
                .stream().filter(LaundryService::isActive).toList());
    }

    private void refreshCustomerCombo() {
        customerComboBox.setItems(customerService.findAll());
    }

    private void addItemToCart(LaundryService service) {
        for (OrderItem item : cartItems) {
            if (item.getService() != null && item.getService().getId().equals(service.getId())) {
                item.setQuantity(item.getQuantity().add(BigDecimal.ONE));
                item.setSubtotal(item.getUnitPrice().multiply(item.getQuantity()));
                renderCartItems();
                return;
            }
        }

        OrderItem newItem = new OrderItem(service, BigDecimal.ONE, "");
        cartItems.add(newItem);

        estDatePicker.setValue(orderService.calculateDefaultEstimatedDate(cartItems));
        renderCartItems();
    }

    private void renderCartItems() {
        cartItemsContainer.removeAll();

        if (cartItems.isEmpty()) {
            Span empty = new Span("Belum ada layanan dipilih. Pilih layanan pada panel sebelah kiri.");
            empty.getStyle().set("color", "#9C96A8").set("font-style", "italic").set("font-size", "0.85rem");
            cartItemsContainer.add(empty);
            recalculateCartTotals();
            return;
        }

        for (int i = 0; i < cartItems.size(); i++) {
            final int index = i;
            OrderItem item = cartItems.get(i);

            HorizontalLayout itemRow = new HorizontalLayout();
            itemRow.setWidthFull();
            itemRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            itemRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            itemRow.getStyle().set("padding", "0.6rem 0.8rem")
                    .set("border-radius", "8px")
                    .set("background-color", "#F9F6FA")
                    .set("border", "1px solid #EFE7F3");

            VerticalLayout nameAndNote = new VerticalLayout();
            nameAndNote.setPadding(false);
            nameAndNote.setSpacing(false);

            Span nameSpan = new Span(item.getServiceName());
            nameSpan.getStyle().set("font-weight", "600").set("font-size", "0.9rem");
            Span rateSpan = new Span("@" + ReceiptService.formatRupiah(item.getUnitPrice()) + " / " + item.getUnit().name().toLowerCase());
            rateSpan.getStyle().set("font-size", "0.75rem").set("color", "#6C667A");
            nameAndNote.add(nameSpan, rateSpan);

            BigDecimalField qtyField = new BigDecimalField();
            qtyField.setWidth("80px");
            qtyField.setValue(item.getQuantity());
            qtyField.setValueChangeMode(ValueChangeMode.LAZY);
            qtyField.addValueChangeListener(e -> {
                if (e.getValue() != null && e.getValue().compareTo(BigDecimal.ZERO) > 0) {
                    item.setQuantity(e.getValue());
                    item.setSubtotal(item.getUnitPrice().multiply(item.getQuantity()));
                    recalculateCartTotals();
                }
            });

            Span subtotalSpan = new Span(ReceiptService.formatRupiah(item.getSubtotal()));
            subtotalSpan.getStyle().set("font-weight", "800").set("font-size", "0.9rem").set("color", "#7B2D8E");

            Button delBtn = new Button(VaadinIcon.CLOSE.create(), e -> {
                cartItems.remove(index);
                renderCartItems();
            });
            delBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

            HorizontalLayout right = new HorizontalLayout(qtyField, subtotalSpan, delBtn);
            right.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

            itemRow.add(nameAndNote, right);
            cartItemsContainer.add(itemRow);
        }

        recalculateCartTotals();
    }

    private void recalculateCartTotals() {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : cartItems) {
            total = total.add(item.getSubtotal());
        }
        totalAmountSpan.setText(ReceiptService.formatRupiah(total));

        BigDecimal paid = paidAmountField.getValue() != null ? paidAmountField.getValue() : BigDecimal.ZERO;

        paymentStatusBadge.removeClassName("badge-lunas");
        paymentStatusBadge.removeClassName("badge-belum-lunas");
        paymentStatusBadge.removeClassName("badge-sebagian");

        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            paymentStatusBadge.setText("Belum Lunas");
            paymentStatusBadge.addClassName("badge-belum-lunas");
            balanceInfoSpan.setText("Sisa tagihan: " + ReceiptService.formatRupiah(total));
            balanceInfoSpan.getStyle().set("color", "#C62828");
        } else if (paid.compareTo(total) >= 0) {
            paymentStatusBadge.setText("Lunas");
            paymentStatusBadge.addClassName("badge-lunas");
            BigDecimal kembalian = paid.subtract(total);
            if (kembalian.compareTo(BigDecimal.ZERO) > 0) {
                balanceInfoSpan.setText("Kembalian: " + ReceiptService.formatRupiah(kembalian));
                balanceInfoSpan.getStyle().set("color", "#2E7D32");
            } else {
                balanceInfoSpan.setText("Lunas (Pas)");
                balanceInfoSpan.getStyle().set("color", "#2E7D32");
            }
        } else {
            paymentStatusBadge.setText("Sebagian");
            paymentStatusBadge.addClassName("badge-sebagian");
            BigDecimal sisa = total.subtract(paid);
            balanceInfoSpan.setText("Sisa tagihan: " + ReceiptService.formatRupiah(sisa));
            balanceInfoSpan.getStyle().set("color", "#E65100");
        }
    }

    private void openInlineCustomerModal() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Tambah Pelanggan Baru");

        TextField nameField = new TextField("Nama Lengkap");
        nameField.setRequired(true);
        nameField.setWidthFull();

        TextField phoneField = new TextField("Nomor Telepon (WhatsApp)");
        phoneField.setRequired(true);
        phoneField.setPlaceholder("081234567890");
        phoneField.setWidthFull();

        TextArea addressField = new TextArea("Alamat Lengkap");
        addressField.setWidthFull();

        VerticalLayout form = new VerticalLayout(nameField, phoneField, addressField);
        dialog.add(form);

        Button saveBtn = new Button("Simpan & Pilih", e -> {
            try {
                Customer created = customerService.createCustomer(nameField.getValue(), phoneField.getValue(), addressField.getValue());
                refreshCustomerCombo();
                customerComboBox.setValue(created);
                showNotification("Pelanggan berhasil ditambahkan", NotificationVariant.LUMO_SUCCESS);
                dialog.close();
            } catch (Exception ex) {
                showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.getStyle().set("background-color", "#7B2D8E");

        Button cancelBtn = new Button("Batal", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void submitOrder() {
        try {
            Customer customer = customerComboBox.getValue();
            User cashier = authenticatedUser.get().orElse(null);
            LocalDateTime estDate = estDatePicker.getValue();
            PaymentMethod method = paymentMethodRadio.getValue();
            BigDecimal paid = paidAmountField.getValue();
            String notes = generalNotesField.getValue();

            Order savedOrder = orderService.createOrder(customer, cashier, cartItems, estDate, method, paid, notes);
            showNotification("Order berhasil disimpan! No: " + savedOrder.getInvoiceNumber(), NotificationVariant.LUMO_SUCCESS);

            openOrderConfirmationModal(savedOrder);
        } catch (Exception ex) {
            showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void openOrderConfirmationModal(Order order) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Pesanan Berhasil Disimpan! 🎉");
        dialog.setWidth("480px");
        dialog.setMaxWidth("95vw");

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);

        layout.add(new Paragraph("No. Invoice: " + order.getInvoiceNumber()));
        layout.add(new Paragraph("Pelanggan: " + order.getCustomer().getName() + " (" + order.getCustomer().getPhone() + ")"));
        layout.add(new Paragraph("Total: " + ReceiptService.formatRupiah(order.getTotalAmount()) +
                " • Status Bayar: " + order.getPaymentStatus().getDisplayName()));

        Button printBtn = new Button("Cetak Struk Thermal", VaadinIcon.PRINT.create(), e -> {
            ThermalReceiptDialog printDialog = new ThermalReceiptDialog(receiptService, settingService, order);
            printDialog.open();
        });
        printBtn.setWidthFull();
        printBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        printBtn.getStyle().set("background-color", "#7B2D8E");

        Button waBtn = new Button("Kirim Struk via WhatsApp", VaadinIcon.CHAT.create(), e -> {
            try {
                String waUrl = receiptService.generateWhatsAppUrl(order);
                UI.getCurrent().getPage().open(waUrl, "_blank");
            } catch (Exception ex) {
                showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        waBtn.setWidthFull();
        waBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        Button newOrderBtn = new Button("Buat Order Lain", VaadinIcon.PLUS.create(), e -> {
            dialog.close();
            cartItems.clear();
            renderCartItems();
            customerComboBox.clear();
            generalNotesField.clear();
            paidAmountField.setValue(BigDecimal.ZERO);
        });
        newOrderBtn.setWidthFull();

        Button viewOrdersBtn = new Button("Lihat Daftar Order", VaadinIcon.LIST.create(), e -> {
            dialog.close();
            UI.getCurrent().navigate(OrderListView.class);
        });
        viewOrdersBtn.setWidthFull();
        viewOrdersBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        layout.add(printBtn, waBtn, newOrderBtn, viewOrdersBtn);
        dialog.add(layout);
        dialog.open();
    }

    private void showNotification(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
