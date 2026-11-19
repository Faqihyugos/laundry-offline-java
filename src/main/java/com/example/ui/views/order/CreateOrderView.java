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
import com.vaadin.flow.component.html.*;
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
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Route(value = "orders/new", layout = MainLayout.class)
@PageTitle("POS Kasir | Laundry POS")
@RolesAllowed({"OWNER", "CASHIER"})
public class CreateOrderView extends VerticalLayout {

    private final OrderService orderService;
    private final CustomerService customerService;
    private final ServicePackageService servicePackageService;
    private final ReceiptService receiptService;
    private final SettingService settingService;
    private final AuthenticatedUser authenticatedUser;

    // Catalog state
    private String selectedCategory = "Semua";
    private final TextField searchServiceField = new TextField();
    private final Div serviceGridContainer = new Div();
    private final Div categoryBar = new Div();

    // Cart / Ticket state
    private final List<OrderItem> cartItems = new ArrayList<>();
    private final Div cartItemsContainer = new Div();
    private final ComboBox<Customer> customerComboBox = new ComboBox<>();
    private final DateTimePicker estDatePicker = new DateTimePicker("Estimasi Selesai");
    private final RadioButtonGroup<PaymentMethod> paymentMethodRadio = new RadioButtonGroup<>();
    private final BigDecimalField paidAmountField = new BigDecimalField("Nominal Bayar (Rp)");
    private final TextArea generalNotesField = new TextArea();

    private final Span subtotalAmountSpan = new Span("Rp 0");
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
        setPadding(false);
        setSpacing(false);
        getStyle().set("background-color", "var(--bg-app)")
                .set("padding", "1rem");

        createPOSLayout();
    }

    private void createPOSLayout() {
        Div posLayout = new Div();
        posLayout.addClassName("pos-layout");

        // 1. Left: Catalog Panel (65%)
        Div catalogPanel = createCatalogPanel();
        catalogPanel.addClassName("pos-catalog-panel");

        // 2. Right: Ticket Panel (35%)
        Div ticketPanel = createTicketPanel();
        ticketPanel.addClassName("pos-ticket-panel");

        posLayout.add(catalogPanel, ticketPanel);
        add(posLayout);
    }

    private Div createCatalogPanel() {
        Div panel = new Div();
        panel.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "12px");

        // Header with search
        Div headerRow = new Div();
        headerRow.getStyle().set("display", "flex").set("justify-content", "space-between").set("align-items", "center").set("flex-wrap", "wrap").set("gap", "10px");

        Div titleGroup = new Div();
        H2 title = new H2("POS Kasir Laundry");
        title.getStyle().set("margin", "0").set("color", "#0F172A").set("font-size", "1.4rem").set("font-weight", "800");
        Span sub = new Span("Pilih paket layanan untuk ditambahkan ke tiket pesanan");
        sub.getStyle().set("font-size", "0.85rem").set("color", "#64748B");
        titleGroup.add(title, sub);

        searchServiceField.setPlaceholder("Cari layanan...");
        searchServiceField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchServiceField.setClearButtonVisible(true);
        searchServiceField.getStyle().set("width", "280px");
        searchServiceField.setValueChangeMode(ValueChangeMode.LAZY);
        searchServiceField.addValueChangeListener(e -> refreshCatalogGrid());

        headerRow.add(titleGroup, searchServiceField);

        // Category Filter Pills
        categoryBar.addClassName("pos-category-bar");
        renderCategoryPills();

        // Service Catalog Cards Grid
        serviceGridContainer.addClassName("pos-catalog-grid");
        refreshCatalogGrid();

        panel.add(headerRow, categoryBar, serviceGridContainer);
        return panel;
    }

    private void renderCategoryPills() {
        categoryBar.removeAll();
        String[] categories = {"Semua", "Kiloan", "Satuan", "Express", "Bedcover"};

        for (String cat : categories) {
            Span pill = new Span(cat);
            pill.addClassName("category-pill");
            if (cat.equalsIgnoreCase(selectedCategory)) {
                pill.addClassName("active");
            }
            pill.addClickListener(e -> {
                selectedCategory = cat;
                renderCategoryPills();
                refreshCatalogGrid();
            });
            categoryBar.add(pill);
        }
    }

    private void refreshCatalogGrid() {
        serviceGridContainer.removeAll();
        String query = searchServiceField.getValue() != null ? searchServiceField.getValue().trim() : "";

        List<LaundryService> services = servicePackageService.search(query).stream()
                .filter(LaundryService::isActive)
                .filter(s -> {
                    if ("Semua".equalsIgnoreCase(selectedCategory)) return true;
                    if ("Kiloan".equalsIgnoreCase(selectedCategory)) return s.getUnit() == ServiceUnit.KG;
                    if ("Satuan".equalsIgnoreCase(selectedCategory)) return s.getUnit() == ServiceUnit.PCS;
                    if ("Express".equalsIgnoreCase(selectedCategory)) return s.getName().toLowerCase().contains("express") || s.getName().toLowerCase().contains("kilat");
                    if ("Bedcover".equalsIgnoreCase(selectedCategory)) return s.getName().toLowerCase().contains("bedcover") || s.getName().toLowerCase().contains("selimut");
                    return true;
                }).toList();

        if (services.isEmpty()) {
            Div emptyMsg = new Div();
            emptyMsg.setText("Tidak ada layanan yang sesuai dengan kategori / pencarian.");
            emptyMsg.getStyle().set("color", "#94A3B8").set("padding", "3rem 1rem").set("text-align", "center").set("grid-column", "1 / -1");
            serviceGridContainer.add(emptyMsg);
            return;
        }

        for (LaundryService service : services) {
            Div card = createServiceCard(service);
            serviceGridContainer.add(card);
        }
    }

    private Div createServiceCard(LaundryService service) {
        Div card = new Div();
        card.addClassName("pos-service-card");
        card.addClickListener(e -> addItemToCart(service));

        Div cardHeader = new Div();
        cardHeader.addClassName("pos-service-card-header");

        Icon icon = getServiceIcon(service.getName(), service.getUnit());
        icon.setSize("22px");
        icon.setColor("#4F46E5");

        Div iconBox = new Div();
        iconBox.addClassName("pos-service-icon-box");
        iconBox.add(icon);

        Span unitPill = new Span(service.getUnit() == ServiceUnit.KG ? "Kiloan" : "Satuan");
        unitPill.addClassName("pos-service-unit-pill");

        cardHeader.add(iconBox, unitPill);

        Div name = new Div();
        name.addClassName("pos-service-name");
        name.setText(service.getName());

        HorizontalLayout duration = new HorizontalLayout();
        Icon clockIcon = VaadinIcon.CLOCK.create();
        clockIcon.setSize("12px");
        clockIcon.setColor("#64748B");
        Span durationText = new Span(service.getEstimatedDurationHours() + " Jam Pengerjaan");
        durationText.getStyle().set("font-size", "0.75rem").set("color", "#64748B").set("font-weight", "500");
        duration.add(clockIcon, durationText);
        duration.setSpacing(false);
        duration.getStyle().set("gap", "4px").set("align-items", "center").set("margin-top", "2px");

        Div footer = new Div();
        footer.addClassName("pos-service-footer");

        Div priceGroup = new Div();
        Span price = new Span(ReceiptService.formatRupiah(service.getPrice()));
        price.addClassName("pos-service-price");
        Span unit = new Span(" / " + service.getUnit().name().toLowerCase());
        unit.addClassName("pos-service-unit");
        priceGroup.add(price, unit);

        Button addBtn = new Button("Tambah", VaadinIcon.PLUS.create());
        addBtn.addClassName("pos-card-add-btn");

        footer.add(priceGroup, addBtn);
        card.add(cardHeader, name, duration, footer);
        return card;
    }

    private Icon getServiceIcon(String name, ServiceUnit unit) {
        String lower = name.toLowerCase();
        if (lower.contains("express") || lower.contains("kilat")) return VaadinIcon.FLASH.create();
        if (lower.contains("bedcover") || lower.contains("selimut") || lower.contains("sprei")) return VaadinIcon.BED.create();
        if (lower.contains("jas") || lower.contains("kemeja") || lower.contains("blazer")) return VaadinIcon.SPECIALIST.create();
        if (lower.contains("sepatu")) return VaadinIcon.STEP_FORWARD.create();
        if (lower.contains("boneka") || lower.contains("gordyn")) return VaadinIcon.PACKAGE.create();
        if (lower.contains("setrika")) return VaadinIcon.MAGIC.create();
        if (unit == ServiceUnit.KG) return VaadinIcon.SCALE.create();
        return VaadinIcon.TAG.create();
    }

    private Div createTicketPanel() {
        Div panel = new Div();

        // 1. Header: Customer Select
        Div header = new Div();
        header.addClassName("pos-ticket-header");

        Div titleRow = new Div();
        titleRow.addClassName("pos-ticket-title");
        Icon ticketIcon = VaadinIcon.INVOICE.create();
        ticketIcon.setSize("20px");
        ticketIcon.setColor("#4F46E5");
        Span t1 = new Span("Tiket Pesanan");
        HorizontalLayout titleLeft = new HorizontalLayout(ticketIcon, t1);
        titleLeft.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        titleLeft.getStyle().set("gap", "8px");
        Span itemCountBadge = new Span(cartItems.size() + " Item");
        itemCountBadge.addClassNames("status-badge", "badge-role-owner");
        titleRow.add(titleLeft, itemCountBadge);

        customerComboBox.setWidthFull();
        customerComboBox.setPlaceholder("Pilih atau cari pelanggan...");
        customerComboBox.setItemLabelGenerator(c -> c.getName() + " (" + c.getPhone() + ")");
        refreshCustomerCombo();

        Button addCustomerBtn = new Button(VaadinIcon.PLUS.create(), e -> openInlineCustomerModal());
        addCustomerBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addCustomerBtn.getStyle().set("background", "var(--primary-gradient)");
        addCustomerBtn.setTooltipText("Tambah Pelanggan Baru");

        HorizontalLayout customerRow = new HorizontalLayout(customerComboBox, addCustomerBtn);
        customerRow.setWidthFull();
        customerRow.getStyle().set("margin-top", "10px");
        customerRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        header.add(titleRow, customerRow);

        // 2. Middle: Scrollable Items List
        cartItemsContainer.addClassName("pos-ticket-items-scroll");
        renderCartItems();

        // 3. Footer: Calculations, notes, quick cash, & checkout button
        Div footer = new Div();
        footer.addClassName("pos-ticket-footer");

        generalNotesField.setPlaceholder("Catatan cucian / parfum...");
        generalNotesField.setWidthFull();
        generalNotesField.setMaxLength(255);
        generalNotesField.getStyle().set("font-size", "0.82rem");

        estDatePicker.setWidthFull();
        estDatePicker.setValue(LocalDateTime.now().plusHours(24));

        HorizontalLayout notesAndDate = new HorizontalLayout(generalNotesField, estDatePicker);
        notesAndDate.setWidthFull();
        notesAndDate.setSpacing(true);

        paymentMethodRadio.setItems(PaymentMethod.CASH, PaymentMethod.TRANSFER);
        paymentMethodRadio.setItemLabelGenerator(PaymentMethod::getDisplayName);
        paymentMethodRadio.setValue(PaymentMethod.CASH);

        paidAmountField.setWidthFull();
        paidAmountField.setPrefixComponent(new Span("Rp "));
        paidAmountField.setValue(BigDecimal.ZERO);
        paidAmountField.setValueChangeMode(ValueChangeMode.EAGER);
        paidAmountField.addValueChangeListener(e -> recalculateCartTotals());

        // Quick Cash Chips
        Div quickCashRow = new Div();
        quickCashRow.addClassName("quick-cash-row");
        Button pasBtn = new Button("Uang Pas", e -> {
            paidAmountField.setValue(getCurrentTotal());
            recalculateCartTotals();
        });
        pasBtn.addClassName("quick-cash-chip");
        Button k20Btn = new Button("20k", e -> {
            paidAmountField.setValue(new BigDecimal("20000"));
            recalculateCartTotals();
        });
        k20Btn.addClassName("quick-cash-chip");
        Button k50Btn = new Button("50k", e -> {
            paidAmountField.setValue(new BigDecimal("50000"));
            recalculateCartTotals();
        });
        k50Btn.addClassName("quick-cash-chip");
        Button k100Btn = new Button("100k", e -> {
            paidAmountField.setValue(new BigDecimal("100000"));
            recalculateCartTotals();
        });
        k100Btn.addClassName("quick-cash-chip");
        quickCashRow.add(pasBtn, k20Btn, k50Btn, k100Btn);

        paymentStatusBadge.addClassNames("status-badge", "badge-belum-lunas");

        // High-Tech Total Banner
        Div totalBanner = new Div();
        totalBanner.addClassName("pos-total-banner");

        Div bannerTop = new Div();
        bannerTop.addClassName("pos-total-banner-top");
        Span bannerTitle = new Span("Total Tagihan");
        bannerTitle.getStyle().set("font-size", "0.82rem").set("font-weight", "600").set("color", "#94A3B8");
        bannerTop.add(bannerTitle, totalAmountSpan);
        totalAmountSpan.addClassName("pos-total-val");

        Div bannerBottom = new Div();
        bannerBottom.addClassName("pos-total-banner-bottom");
        bannerBottom.add(paymentStatusBadge, balanceInfoSpan);
        balanceInfoSpan.getStyle().set("font-size", "0.82rem").set("font-weight", "700").set("color", "#E2E8F0");

        totalBanner.add(bannerTop, bannerBottom);

        Button checkoutBtn = new Button("Bayar & Cetak Struk", VaadinIcon.ARROW_RIGHT.create(), e -> submitOrder());
        checkoutBtn.setIconAfterText(true);
        checkoutBtn.addClassName("pos-checkout-btn");

        footer.add(notesAndDate, paymentMethodRadio, paidAmountField, quickCashRow, totalBanner, checkoutBtn);

        panel.add(header, cartItemsContainer, footer);
        return panel;
    }

    private BigDecimal getCurrentTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : cartItems) {
            total = total.add(item.getSubtotal());
        }
        return total;
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
            Div emptyBox = new Div();
            emptyBox.getStyle().set("text-align", "center").set("padding", "2rem 1rem").set("color", "#94A3B8");
            Icon icon = VaadinIcon.CART.create();
            icon.setSize("32px");
            icon.setColor("#CBD5E1");
            icon.getStyle().set("display", "block").set("margin", "0 auto 8px auto");
            Span txt = new Span("Keranjang masih kosong");
            txt.getStyle().set("font-size", "0.9rem").set("font-weight", "600").set("display", "block");
            Span subTxt = new Span("Pilih layanan di sebelah kiri untuk menambahkan");
            subTxt.getStyle().set("font-size", "0.75rem").set("color", "#94A3B8");
            emptyBox.add(icon, txt, subTxt);
            cartItemsContainer.add(emptyBox);
            recalculateCartTotals();
            return;
        }

        for (int i = 0; i < cartItems.size(); i++) {
            final int index = i;
            OrderItem item = cartItems.get(i);

            Div itemRow = new Div();
            itemRow.addClassName("pos-cart-item");

            // Left: Name & rate
            Div left = new Div();
            left.getStyle().set("flex", "1 1 auto");
            Div name = new Div();
            name.addClassName("pos-cart-item-name");
            name.setText(item.getServiceName());

            Div rate = new Div();
            rate.addClassName("pos-cart-item-rate");
            rate.setText("@" + ReceiptService.formatRupiah(item.getUnitPrice()) + " / " + item.getUnit().name().toLowerCase());
            left.add(name, rate);

            // Center: Stepper [-] [qty] [+]
            Div stepper = new Div();
            stepper.addClassName("pos-cart-item-stepper");

            Button minusBtn = new Button("-", e -> {
                if (item.getQuantity().compareTo(BigDecimal.ONE) > 0) {
                    item.setQuantity(item.getQuantity().subtract(BigDecimal.ONE));
                    item.setSubtotal(item.getUnitPrice().multiply(item.getQuantity()));
                    renderCartItems();
                } else {
                    cartItems.remove(index);
                    renderCartItems();
                }
            });
            minusBtn.addClassName("pos-stepper-btn");

            Span qtySpan = new Span(ReceiptService.formatQuantity(item.getQuantity()));
            qtySpan.getStyle().set("font-size", "0.85rem").set("font-weight", "700").set("min-width", "24px").set("text-align", "center");

            Button plusBtn = new Button("+", e -> {
                item.setQuantity(item.getQuantity().add(BigDecimal.ONE));
                item.setSubtotal(item.getUnitPrice().multiply(item.getQuantity()));
                renderCartItems();
            });
            plusBtn.addClassName("pos-stepper-btn");

            stepper.add(minusBtn, qtySpan, plusBtn);

            // Right: Subtotal & Delete
            Span subtotal = new Span(ReceiptService.formatRupiah(item.getSubtotal()));
            subtotal.addClassName("pos-cart-item-subtotal");

            Button delBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                cartItems.remove(index);
                renderCartItems();
            });
            delBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

            itemRow.add(left, stepper, subtotal, delBtn);
            cartItemsContainer.add(itemRow);
        }

        recalculateCartTotals();
    }

    private void recalculateCartTotals() {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : cartItems) {
            total = total.add(item.getSubtotal());
        }
        subtotalAmountSpan.setText(ReceiptService.formatRupiah(total));
        totalAmountSpan.setText(ReceiptService.formatRupiah(total));

        BigDecimal paid = paidAmountField.getValue() != null ? paidAmountField.getValue() : BigDecimal.ZERO;

        paymentStatusBadge.removeClassName("badge-lunas");
        paymentStatusBadge.removeClassName("badge-belum-lunas");
        paymentStatusBadge.removeClassName("badge-sebagian");

        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            paymentStatusBadge.setText("Belum Lunas");
            paymentStatusBadge.addClassName("badge-belum-lunas");
            balanceInfoSpan.setText("Sisa tagihan: " + ReceiptService.formatRupiah(total));
            balanceInfoSpan.getStyle().set("color", "#DC2626");
        } else if (paid.compareTo(total) >= 0) {
            paymentStatusBadge.setText("Lunas");
            paymentStatusBadge.addClassName("badge-lunas");
            BigDecimal kembalian = paid.subtract(total);
            if (kembalian.compareTo(BigDecimal.ZERO) > 0) {
                balanceInfoSpan.setText("Kembalian: " + ReceiptService.formatRupiah(kembalian));
                balanceInfoSpan.getStyle().set("color", "#059669");
            } else {
                balanceInfoSpan.setText("Lunas (Pas)");
                balanceInfoSpan.getStyle().set("color", "#059669");
            }
        } else {
            paymentStatusBadge.setText("Sebagian");
            paymentStatusBadge.addClassName("badge-sebagian");
            BigDecimal sisa = total.subtract(paid);
            balanceInfoSpan.setText("Sisa tagihan: " + ReceiptService.formatRupiah(sisa));
            balanceInfoSpan.getStyle().set("color", "#D97706");
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
        saveBtn.getStyle().set("background-color", "#6D28D9");

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
        dialog.setHeaderTitle("Pesanan Berhasil Disimpan");
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
        printBtn.getStyle().set("background-color", "#6D28D9");

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

        Button viewOrdersBtn = new Button("Lihat Daftar Antrian Order", VaadinIcon.LIST.create(), e -> {
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
