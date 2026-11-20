package com.example.ui.views.order;

import com.example.domain.*;
import com.example.security.AuthenticatedUser;
import com.example.service.*;
import com.example.ui.MainLayout;
import com.example.ui.components.QRSvgHelper;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    // Quick Action Bar States (Figma match)
    private String orderNotes = "";
    private int discountPercentage = 0;
    private BigDecimal discountNominal = BigDecimal.ZERO;
    private String discountLabel = "Tanpa Diskon";
    private int taxPercentage = 0;
    private String taxLabel = "Tanpa Pajak";

    // Quick Action Buttons
    private final Div noteBtn = new Div();
    private final Div discountBtn = new Div();
    private final Div taxBtn = new Div();
    private final Div customerBtn = new Div();

    // Financial Summary Elements
    private final Span subtotalAmountSpan = new Span("Rp 0");
    private final Div discountSummaryRow = new Div();
    private final Span discountAmountSpan = new Span("- Rp 0");
    private final Div taxSummaryRow = new Div();
    private final Span taxAmountSpan = new Span("+ Rp 0");
    private final Span totalAmountSpan = new Span("Rp 0");
    private final Span itemCountBadge = new Span("0 Item");

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

        // Header with title and search field
        Div headerRow = new Div();
        headerRow.getStyle().set("display", "flex").set("justify-content", "space-between").set("align-items", "center").set("flex-wrap", "wrap").set("gap", "10px");

        Div titleGroup = new Div();
        String laundryName = settingService.getSetting(SettingService.KEY_LAUNDRY_NAME, "CWB Laundry");
        H2 title = new H2(laundryName + " POS");
        title.getStyle().set("margin", "0").set("color", "var(--text-main)").set("font-size", "1.45rem").set("font-weight", "800").set("letter-spacing", "-0.02em");

        LocalDate today = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", new Locale("id", "ID"));
        Span sub = new Span(today.format(dtf) + " • Kasir Siap Melayani");
        sub.getStyle().set("font-size", "0.82rem").set("color", "#64748B").set("font-weight", "500");
        titleGroup.add(title, sub);

        searchServiceField.setPlaceholder("Cari layanan...");
        searchServiceField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchServiceField.setClearButtonVisible(true);
        searchServiceField.getStyle().set("width", "280px");
        searchServiceField.setValueChangeMode(ValueChangeMode.LAZY);
        searchServiceField.addValueChangeListener(e -> refreshCatalogGrid());

        headerRow.add(titleGroup, searchServiceField);

        // Category Filter Pills (Figma match)
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
            emptyMsg.setText("Tidak ada paket layanan yang sesuai dengan kategori / pencarian.");
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
        icon.setColor("#283BB0");

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

        // 1. Header: Order Title & Customer Combo
        Div header = new Div();
        header.addClassName("pos-ticket-header");

        Div titleRow = new Div();
        titleRow.addClassName("pos-ticket-title");
        Icon ticketIcon = VaadinIcon.INVOICE.create();
        ticketIcon.setSize("20px");
        ticketIcon.setColor("#283BB0");
        Span t1 = new Span("Tiket Pesanan");
        HorizontalLayout titleLeft = new HorizontalLayout(ticketIcon, t1);
        titleLeft.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        titleLeft.getStyle().set("gap", "8px");

        itemCountBadge.addClassNames("status-badge", "badge-role-owner");
        itemCountBadge.setText(cartItems.size() + " Item");
        titleRow.add(titleLeft, itemCountBadge);

        customerComboBox.setWidthFull();
        customerComboBox.setPlaceholder("Pilih atau cari pelanggan...");
        customerComboBox.setItemLabelGenerator(c -> c.getName() + " (" + c.getPhone() + ")");
        customerComboBox.addValueChangeListener(e -> updateQuickActionButtons());
        refreshCustomerCombo();

        Button addCustomerBtn = new Button(VaadinIcon.PLUS.create(), e -> openCustomerModal());
        addCustomerBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addCustomerBtn.getStyle().set("background", "var(--primary-gradient)");
        addCustomerBtn.setTooltipText("Tambah Pelanggan Baru");

        HorizontalLayout customerRow = new HorizontalLayout(customerComboBox, addCustomerBtn);
        customerRow.setWidthFull();
        customerRow.getStyle().set("margin-top", "10px");
        customerRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        header.add(titleRow, customerRow);

        // 2. Middle: Scrollable Cart Items List
        cartItemsContainer.addClassName("pos-ticket-items-scroll");
        renderCartItems();

        // 3. Footer: Quick Action Buttons, Calculations, & Checkout
        Div footer = new Div();
        footer.addClassName("pos-ticket-footer");

        // Quick Action Bar (Catatan, Diskon, Pajak, Pelanggan - Figma match)
        Div quickActionBar = createQuickActionBar();

        // Financial Summary Box
        Div summaryBox = createSummaryBox();

        estDatePicker.setValue(LocalDateTime.now().plusHours(24));
        estDatePicker.setVisible(false); // Managed automatically by default duration

        Button checkoutBtn = new Button("Lanjutkan Pembayaran", VaadinIcon.ARROW_RIGHT.create(), e -> openPaymentModal());
        checkoutBtn.setIconAfterText(true);
        checkoutBtn.addClassName("pos-checkout-btn");

        footer.add(quickActionBar, summaryBox, checkoutBtn);

        panel.add(header, cartItemsContainer, footer);
        return panel;
    }

    private Div createQuickActionBar() {
        Div bar = new Div();
        bar.addClassName("pos-quick-actions");

        setupActionBtn(noteBtn, VaadinIcon.EDIT, "Catatan", e -> openNotesModal());
        setupActionBtn(discountBtn, VaadinIcon.TAG, "Diskon", e -> openDiscountModal());
        setupActionBtn(taxBtn, VaadinIcon.INVOICE, "Pajak", e -> openTaxModal());
        setupActionBtn(customerBtn, VaadinIcon.USER, "Pelanggan", e -> openCustomerModal());

        bar.add(noteBtn, discountBtn, taxBtn, customerBtn);
        updateQuickActionButtons();
        return bar;
    }

    private void setupActionBtn(Div btn, VaadinIcon iconType, String defaultLabel, com.vaadin.flow.component.ComponentEventListener<com.vaadin.flow.component.ClickEvent<Div>> listener) {
        btn.addClassName("pos-action-btn");
        btn.removeAll();

        Div iconBox = new Div();
        iconBox.addClassName("pos-action-btn-icon");
        Icon icon = iconType.create();
        icon.setSize("16px");
        iconBox.add(icon);

        Span label = new Span(defaultLabel);
        label.addClassName("pos-action-btn-label");

        btn.add(iconBox, label);
        btn.addClickListener(listener);
    }

    private void updateQuickActionButtons() {
        // 1. Note button state
        Span noteLabel = (Span) noteBtn.getComponentAt(1);
        if (orderNotes != null && !orderNotes.trim().isEmpty()) {
            noteBtn.addClassName("active");
            noteLabel.setText("Ada Catatan");
        } else {
            noteBtn.removeClassName("active");
            noteLabel.setText("Catatan");
        }

        // 2. Discount button state
        Span discLabel = (Span) discountBtn.getComponentAt(1);
        if (discountPercentage > 0) {
            discountBtn.addClassName("active");
            discLabel.setText(discountPercentage + "%");
        } else if (discountNominal.compareTo(BigDecimal.ZERO) > 0) {
            discountBtn.addClassName("active");
            discLabel.setText("Diskon");
        } else {
            discountBtn.removeClassName("active");
            discLabel.setText("Diskon");
        }

        // 3. Tax button state
        Span txLabel = (Span) taxBtn.getComponentAt(1);
        if (taxPercentage > 0) {
            taxBtn.addClassName("active");
            txLabel.setText("PPN " + taxPercentage + "%");
        } else {
            taxBtn.removeClassName("active");
            txLabel.setText("Pajak");
        }

        // 4. Customer button state
        Span custLabel = (Span) customerBtn.getComponentAt(1);
        Customer cust = customerComboBox.getValue();
        if (cust != null) {
            customerBtn.addClassName("active");
            String firstName = cust.getName().split(" ")[0];
            custLabel.setText(firstName);
        } else {
            customerBtn.removeClassName("active");
            custLabel.setText("Pelanggan");
        }
    }

    private Div createSummaryBox() {
        Div box = new Div();
        box.addClassName("pos-summary-box");

        // Row Subtotal
        Div subtotalRow = new Div();
        subtotalRow.addClassName("pos-summary-row");
        Span subLabel = new Span("Subtotal");
        subtotalRow.add(subLabel, subtotalAmountSpan);

        // Row Discount (hidden if zero)
        discountSummaryRow.addClassName("pos-summary-row");
        discountSummaryRow.addClassName("discount");
        Span discLabel = new Span("Diskon (" + discountLabel + ")");
        discountSummaryRow.add(discLabel, discountAmountSpan);
        discountSummaryRow.setVisible(false);

        // Row Tax (hidden if zero)
        taxSummaryRow.addClassName("pos-summary-row");
        taxSummaryRow.addClassName("tax");
        Span taxRowLabel = new Span("Pajak (" + taxLabel + ")");
        taxSummaryRow.add(taxRowLabel, taxAmountSpan);
        taxSummaryRow.setVisible(false);

        // Row Total
        Div totalRow = new Div();
        totalRow.addClassName("pos-summary-total-row");
        Span totalLabel = new Span("Total Tagihan");
        totalLabel.addClassName("pos-summary-total-label");
        totalAmountSpan.addClassName("pos-summary-total-val");
        totalRow.add(totalLabel, totalAmountSpan);

        box.add(subtotalRow, discountSummaryRow, taxSummaryRow, totalRow);
        return box;
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
        itemCountBadge.setText(cartItems.size() + " Item");

        if (cartItems.isEmpty()) {
            Div emptyBox = new Div();
            emptyBox.getStyle().set("text-align", "center").set("padding", "2rem 1rem").set("color", "#94A3B8");
            Icon icon = VaadinIcon.CART.create();
            icon.setSize("32px");
            icon.setColor("#CBD5E1");
            icon.getStyle().set("display", "block").set("margin", "0 auto 8px auto");
            Span txt = new Span("Keranjang masih kosong");
            txt.getStyle().set("font-size", "0.9rem").set("font-weight", "600").set("display", "block");
            Span subTxt = new Span("Pilih layanan di katalog untuk menambahkan");
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

    private BigDecimal getCalculatedSubtotal() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : cartItems) {
            subtotal = subtotal.add(item.getSubtotal());
        }
        return subtotal;
    }

    private BigDecimal getCalculatedDiscount(BigDecimal subtotal) {
        if (discountPercentage > 0) {
            return subtotal.multiply(BigDecimal.valueOf(discountPercentage))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return discountNominal != null ? discountNominal : BigDecimal.ZERO;
    }

    private BigDecimal getCalculatedTax(BigDecimal taxableAmount) {
        if (taxPercentage > 0) {
            return taxableAmount.multiply(BigDecimal.valueOf(taxPercentage))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal getFinalTotal() {
        BigDecimal subtotal = getCalculatedSubtotal();
        BigDecimal discount = getCalculatedDiscount(subtotal);
        BigDecimal afterDiscount = subtotal.subtract(discount);
        if (afterDiscount.compareTo(BigDecimal.ZERO) < 0) afterDiscount = BigDecimal.ZERO;
        BigDecimal tax = getCalculatedTax(afterDiscount);
        return afterDiscount.add(tax);
    }

    private void recalculateCartTotals() {
        BigDecimal subtotal = getCalculatedSubtotal();
        subtotalAmountSpan.setText(ReceiptService.formatRupiah(subtotal));

        BigDecimal discount = getCalculatedDiscount(subtotal);
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            discountSummaryRow.setVisible(true);
            discountSummaryRow.removeAll();
            discountSummaryRow.add(new Span("Diskon (" + discountLabel + ")"), discountAmountSpan);
            discountAmountSpan.setText("- " + ReceiptService.formatRupiah(discount));
        } else {
            discountSummaryRow.setVisible(false);
        }

        BigDecimal afterDiscount = subtotal.subtract(discount);
        if (afterDiscount.compareTo(BigDecimal.ZERO) < 0) afterDiscount = BigDecimal.ZERO;

        BigDecimal tax = getCalculatedTax(afterDiscount);
        if (tax.compareTo(BigDecimal.ZERO) > 0) {
            taxSummaryRow.setVisible(true);
            taxSummaryRow.removeAll();
            taxSummaryRow.add(new Span("Pajak (" + taxLabel + ")"), taxAmountSpan);
            taxAmountSpan.setText("+ " + ReceiptService.formatRupiah(tax));
        } else {
            taxSummaryRow.setVisible(false);
        }

        BigDecimal finalTotal = afterDiscount.add(tax);
        totalAmountSpan.setText(ReceiptService.formatRupiah(finalTotal));
        updateQuickActionButtons();
    }

    // ==========================================
    // 4 Modals from Quick Action Bar (Figma Match)
    // ==========================================

    private void openNotesModal() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Catatan Khusus Pesanan");
        dialog.setWidth("440px");

        TextArea noteArea = new TextArea("Instruksi Cuci / Parfum");
        noteArea.setWidthFull();
        noteArea.setPlaceholder("Contoh: Pisahkan pakaian putih, parfum extra wangi lavender...");
        noteArea.setValue(orderNotes != null ? orderNotes : "");
        noteArea.setHeight("120px");

        Button saveBtn = new Button("Simpan Catatan", e -> {
            orderNotes = noteArea.getValue() != null ? noteArea.getValue().trim() : "";
            updateQuickActionButtons();
            dialog.close();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.getStyle().set("background", "var(--primary-gradient)");

        Button cancelBtn = new Button("Batal", e -> dialog.close());
        dialog.add(noteArea);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void openDiscountModal() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Pilih Diskon Pesanan");
        dialog.setWidth("440px");

        RadioButtonGroup<String> discountRadio = new RadioButtonGroup<>();
        discountRadio.setItems(
                "Tanpa Diskon (0%)",
                "Diskon Pelanggan Member (10%)",
                "Promo Spesial Opening (15%)",
                "Potongan Langsung (Rp 5.000)",
                "Potongan Spesial (Rp 10.000)"
        );
        discountRadio.setValue("Tanpa Diskon (0%)");
        if (discountPercentage == 10) discountRadio.setValue("Diskon Pelanggan Member (10%)");
        else if (discountPercentage == 15) discountRadio.setValue("Promo Spesial Opening (15%)");
        else if (discountNominal.compareTo(BigDecimal.valueOf(5000)) == 0) discountRadio.setValue("Potongan Langsung (Rp 5.000)");
        else if (discountNominal.compareTo(BigDecimal.valueOf(10000)) == 0) discountRadio.setValue("Potongan Spesial (Rp 10.000)");

        VerticalLayout layout = new VerticalLayout(discountRadio);
        layout.setPadding(false);

        Button applyBtn = new Button("Terapkan Diskon", e -> {
            String val = discountRadio.getValue();
            if ("Diskon Pelanggan Member (10%)".equals(val)) {
                discountPercentage = 10;
                discountNominal = BigDecimal.ZERO;
                discountLabel = "Member 10%";
            } else if ("Promo Spesial Opening (15%)".equals(val)) {
                discountPercentage = 15;
                discountNominal = BigDecimal.ZERO;
                discountLabel = "Promo 15%";
            } else if ("Potongan Langsung (Rp 5.000)".equals(val)) {
                discountPercentage = 0;
                discountNominal = BigDecimal.valueOf(5000);
                discountLabel = "Rp 5.000";
            } else if ("Potongan Spesial (Rp 10.000)".equals(val)) {
                discountPercentage = 0;
                discountNominal = BigDecimal.valueOf(10000);
                discountLabel = "Rp 10.000";
            } else {
                discountPercentage = 0;
                discountNominal = BigDecimal.ZERO;
                discountLabel = "Tanpa Diskon";
            }
            recalculateCartTotals();
            dialog.close();
        });
        applyBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        applyBtn.getStyle().set("background", "var(--primary-gradient)");

        Button cancelBtn = new Button("Batal", e -> dialog.close());
        dialog.add(layout);
        dialog.getFooter().add(cancelBtn, applyBtn);
        dialog.open();
    }

    private void openTaxModal() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Perhitungan Pajak Pesanan");
        dialog.setWidth("420px");

        RadioButtonGroup<String> taxRadio = new RadioButtonGroup<>();
        taxRadio.setItems("Tanpa Pajak (0%)", "PPN Standar (11%)");
        taxRadio.setValue(taxPercentage == 11 ? "PPN Standar (11%)" : "Tanpa Pajak (0%)");

        VerticalLayout layout = new VerticalLayout(taxRadio);
        layout.setPadding(false);

        Button applyBtn = new Button("Terapkan Pajak", e -> {
            if ("PPN Standar (11%)".equals(taxRadio.getValue())) {
                taxPercentage = 11;
                taxLabel = "PPN 11%";
            } else {
                taxPercentage = 0;
                taxLabel = "Tanpa Pajak";
            }
            recalculateCartTotals();
            dialog.close();
        });
        applyBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        applyBtn.getStyle().set("background", "var(--primary-gradient)");

        Button cancelBtn = new Button("Batal", e -> dialog.close());
        dialog.add(layout);
        dialog.getFooter().add(cancelBtn, applyBtn);
        dialog.open();
    }

    private void openCustomerModal() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Pilih atau Tambah Pelanggan");
        dialog.setWidth("480px");

        ComboBox<Customer> selectCombo = new ComboBox<>("Pilih Pelanggan Terdaftar");
        selectCombo.setWidthFull();
        selectCombo.setItems(customerService.findAll());
        selectCombo.setItemLabelGenerator(c -> c.getName() + " (" + c.getPhone() + ")");
        selectCombo.setValue(customerComboBox.getValue());

        Paragraph orDiv = new Paragraph("— Atau Tambah Pelanggan Baru —");
        orDiv.getStyle().set("text-align", "center").set("color", "#94A3B8").set("font-size", "0.8rem").set("margin", "8px 0");

        TextField nameField = new TextField("Nama Lengkap");
        nameField.setWidthFull();
        nameField.setPlaceholder("Nama pelanggan...");

        TextField phoneField = new TextField("Nomor WhatsApp / Telepon");
        phoneField.setWidthFull();
        phoneField.setPlaceholder("081234567890");

        TextArea addrField = new TextArea("Alamat Lengkap");
        addrField.setWidthFull();
        addrField.setPlaceholder("Alamat pengiriman / penjemputan...");

        VerticalLayout form = new VerticalLayout(selectCombo, orDiv, nameField, phoneField, addrField);
        form.setPadding(false);

        Button saveBtn = new Button("Pilih / Simpan", e -> {
            if (nameField.getValue() != null && !nameField.getValue().trim().isEmpty() &&
                phoneField.getValue() != null && !phoneField.getValue().trim().isEmpty()) {
                try {
                    Customer created = customerService.createCustomer(nameField.getValue(), phoneField.getValue(), addrField.getValue());
                    refreshCustomerCombo();
                    customerComboBox.setValue(created);
                    updateQuickActionButtons();
                    dialog.close();
                    return;
                } catch (Exception ex) {
                    showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
                    return;
                }
            } else if (selectCombo.getValue() != null) {
                customerComboBox.setValue(selectCombo.getValue());
                updateQuickActionButtons();
                dialog.close();
                return;
            }
            showNotification("Pilih pelanggan atau lengkapi formulir pelanggan baru", NotificationVariant.LUMO_WARNING);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.getStyle().set("background", "var(--primary-gradient)");

        Button cancelBtn = new Button("Batal", e -> dialog.close());
        dialog.add(form);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    // ==========================================
    // Payment Modal (QRIS & Cash - Figma Match)
    // ==========================================

    private void openPaymentModal() {
        if (cartItems.isEmpty()) {
            showNotification("Keranjang masih kosong, pilih layanan terlebih dahulu.", NotificationVariant.LUMO_WARNING);
            return;
        }

        Customer customer = customerComboBox.getValue();
        if (customer == null) {
            showNotification("Pilih atau tambahkan pelanggan terlebih dahulu.", NotificationVariant.LUMO_WARNING);
            openCustomerModal();
            return;
        }

        BigDecimal finalTotal = getFinalTotal();

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Pembayaran Pesanan");
        dialog.setWidth("520px");
        dialog.setMaxWidth("95vw");

        Div subTitle = new Div();
        subTitle.getStyle().set("font-size", "0.95rem").set("color", "#64748B").set("margin-bottom", "14px");
        subTitle.setText("Total yang harus dibayar: ");
        Span totalSpan = new Span(ReceiptService.formatRupiah(finalTotal));
        totalSpan.getStyle().set("font-weight", "800").set("color", "#283BB0").set("font-size", "1.1rem");
        subTitle.add(totalSpan);

        // Segmented Tabs: [ QRIS ] and [ Tunai ]
        Div tabsRow = new Div();
        tabsRow.addClassName("payment-tabs");

        Button qrisTabBtn = new Button("QRIS Pay", VaadinIcon.QRCODE.create());
        qrisTabBtn.addClassName("payment-tab-btn");
        qrisTabBtn.addClassName("active");

        Button cashTabBtn = new Button("Tunai (Cash)", VaadinIcon.MONEY.create());
        cashTabBtn.addClassName("payment-tab-btn");

        tabsRow.add(qrisTabBtn, cashTabBtn);

        // Content Containers
        Div qrisContent = new Div();
        Div cashContent = new Div();
        cashContent.setVisible(false);

        // --- 1. QRIS CONTENT ---
        qrisContent.addClassName("qris-card");

        Div qrisLogo = new Div();
        qrisLogo.addClassName("qris-header-logo");
        qrisLogo.setText("QRIS");

        String qrisPayload = "00020101021226600016ID.CO.QRIS.WWW011893600999000000000152045812530336054" +
                finalTotal.toPlainString() + "5802ID5914CWB LAUNDRY6007JAKARTA6304";
        Div qrSvg = QRSvgHelper.createQRISComponent(qrisPayload, 180);

        Paragraph qrisText = new Paragraph("Pindai QRIS menggunakan aplikasi mobile banking atau e-wallet (BCA, Mandiri, BRI, GoPay, OVO, ShopeePay)");
        qrisText.addClassName("qris-instruction");

        Button confirmQrisBtn = new Button("Konfirmasi Pembayaran QRIS", VaadinIcon.CHECK_CIRCLE.create(), e -> {
            try {
                User cashier = authenticatedUser.get().orElse(null);
                LocalDateTime est = estDatePicker.getValue();
                BigDecimal discount = getCalculatedDiscount(getCalculatedSubtotal());
                BigDecimal tax = getCalculatedTax(getCalculatedSubtotal().subtract(discount));

                Order saved = orderService.createOrder(customer, cashier, cartItems, est, PaymentMethod.TRANSFER, finalTotal, orderNotes, discount, tax);
                dialog.close();
                openSuccessStrukModal(saved, finalTotal, finalTotal, BigDecimal.ZERO, "QRIS (Lunas)");
            } catch (Exception ex) {
                showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        confirmQrisBtn.addClassName("pos-checkout-btn");

        qrisContent.add(qrisLogo, qrSvg, qrisText, confirmQrisBtn);

        // --- 2. CASH CONTENT ---
        cashContent.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "12px");

        BigDecimalField paidField = new BigDecimalField("Uang Diterima (Rp)");
        paidField.setWidthFull();
        paidField.setPrefixComponent(new Span("Rp "));
        paidField.setValue(finalTotal);
        paidField.setValueChangeMode(ValueChangeMode.EAGER);

        Div quickCashRow = new Div();
        quickCashRow.addClassName("quick-cash-row");

        Button pasBtn = new Button("Uang Pas", e -> paidField.setValue(finalTotal));
        pasBtn.addClassName("quick-cash-chip");

        Button k20Btn = new Button("20k", e -> paidField.setValue(BigDecimal.valueOf(20000)));
        k20Btn.addClassName("quick-cash-chip");

        Button k50Btn = new Button("50k", e -> paidField.setValue(BigDecimal.valueOf(50000)));
        k50Btn.addClassName("quick-cash-chip");

        Button k100Btn = new Button("100k", e -> paidField.setValue(BigDecimal.valueOf(100000)));
        k100Btn.addClassName("quick-cash-chip");

        quickCashRow.add(pasBtn, k20Btn, k50Btn, k100Btn);

        Div cashSummaryBox = new Div();
        cashSummaryBox.addClassName("pos-summary-box");
        Span changeSpan = new Span("Kembalian: Rp 0");
        changeSpan.getStyle().set("font-weight", "800").set("color", "#059669").set("font-size", "1.05rem");
        cashSummaryBox.add(changeSpan);

        paidField.addValueChangeListener(e -> {
            BigDecimal paid = e.getValue() != null ? e.getValue() : BigDecimal.ZERO;
            if (paid.compareTo(finalTotal) >= 0) {
                BigDecimal kembalian = paid.subtract(finalTotal);
                changeSpan.setText("Kembalian: " + ReceiptService.formatRupiah(kembalian));
                changeSpan.getStyle().set("color", "#059669");
            } else {
                BigDecimal sisa = finalTotal.subtract(paid);
                changeSpan.setText("Sisa Tagihan (DP): " + ReceiptService.formatRupiah(sisa));
                changeSpan.getStyle().set("color", "#D97706");
            }
        });

        Button payCashBtn = new Button("Bayar & Cetak Struk", VaadinIcon.CHECK.create(), e -> {
            try {
                BigDecimal paid = paidField.getValue() != null ? paidField.getValue() : BigDecimal.ZERO;
                User cashier = authenticatedUser.get().orElse(null);
                LocalDateTime est = estDatePicker.getValue();
                BigDecimal discount = getCalculatedDiscount(getCalculatedSubtotal());
                BigDecimal tax = getCalculatedTax(getCalculatedSubtotal().subtract(discount));

                Order saved = orderService.createOrder(customer, cashier, cartItems, est, PaymentMethod.CASH, paid, orderNotes, discount, tax);
                dialog.close();

                BigDecimal kembalian = paid.compareTo(finalTotal) >= 0 ? paid.subtract(finalTotal) : BigDecimal.ZERO;
                openSuccessStrukModal(saved, finalTotal, paid, kembalian, "Tunai (" + saved.getPaymentStatus().getDisplayName() + ")");
            } catch (Exception ex) {
                showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        payCashBtn.addClassName("pos-checkout-btn");

        cashContent.add(paidField, quickCashRow, cashSummaryBox, payCashBtn);

        // Tab Switching Actions
        qrisTabBtn.addClickListener(e -> {
            qrisTabBtn.addClassName("active");
            cashTabBtn.removeClassName("active");
            qrisContent.setVisible(true);
            cashContent.setVisible(false);
        });

        cashTabBtn.addClickListener(e -> {
            cashTabBtn.addClassName("active");
            qrisTabBtn.removeClassName("active");
            qrisContent.setVisible(false);
            cashContent.setVisible(true);
        });

        VerticalLayout container = new VerticalLayout(subTitle, tabsRow, qrisContent, cashContent);
        container.setPadding(false);

        Button closeBtn = new Button("Batal", e -> dialog.close());
        dialog.add(container);
        dialog.getFooter().add(closeBtn);
        dialog.open();
    }

    // ==========================================
    // Struk n Print Modal (Figma Match)
    // ==========================================

    private void openSuccessStrukModal(Order order, BigDecimal total, BigDecimal paid, BigDecimal kembalian, String method) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Pembayaran Selesai");
        dialog.setWidth("480px");
        dialog.setMaxWidth("95vw");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        Div checkBadge = new Div();
        checkBadge.addClassName("success-check-badge");
        Icon checkIcon = VaadinIcon.CHECK.create();
        checkIcon.setSize("28px");
        checkBadge.add(checkIcon);

        H3 successTitle = new H3("Pembayaran Berhasil Dilakukan");
        successTitle.getStyle().set("text-align", "center").set("margin", "0").set("color", "#0F172A").set("font-weight", "800");

        Paragraph successSub = new Paragraph("Pesanan telah tersimpan ke dalam antrian pengerjaan.");
        successSub.getStyle().set("text-align", "center").set("color", "#64748B").set("font-size", "0.82rem").set("margin", "0 0 12px 0");

        Div card = new Div();
        card.addClassName("success-receipt-card");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", new Locale("id", "ID"));
        String timeStr = order.getOrderDate() != null ? order.getOrderDate().format(dtf) : LocalDateTime.now().format(dtf);

        card.add(createReceiptRow("No. Invoice", order.getInvoiceNumber()));
        card.add(createReceiptRow("Pelanggan", order.getCustomer().getName() + " (" + order.getCustomer().getPhone() + ")"));
        card.add(createReceiptRow("Metode Pembayaran", method));
        card.add(createReceiptRow("Total Tagihan", ReceiptService.formatRupiah(total)));
        card.add(createReceiptRow("Uang Diterima", ReceiptService.formatRupiah(paid)));
        card.add(createReceiptRow("Kembalian", ReceiptService.formatRupiah(kembalian)));
        card.add(createReceiptRow("Waktu Transaksi", timeStr));

        layout.add(checkBadge, successTitle, successSub, card);

        Button printBtn = new Button("Cetak Struk Thermal", VaadinIcon.PRINT.create(), e -> {
            ThermalReceiptDialog printDialog = new ThermalReceiptDialog(receiptService, settingService, order);
            printDialog.open();
        });
        printBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        printBtn.getStyle().set("background", "var(--primary-gradient)");

        Button waBtn = new Button("Kirim WhatsApp", VaadinIcon.CHAT.create(), e -> {
            String url = receiptService.generateWhatsAppUrl(order);
            UI.getCurrent().getPage().open(url, "_blank");
        });
        waBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        Button newOrderBtn = new Button("Pesanan Baru", VaadinIcon.REFRESH.create(), e -> {
            resetPOS();
            dialog.close();
        });

        HorizontalLayout actions = new HorizontalLayout(printBtn, waBtn, newOrderBtn);
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        dialog.add(layout);
        dialog.getFooter().add(actions);
        dialog.open();
    }

    private Div createReceiptRow(String label, String value) {
        Div row = new Div();
        row.getStyle().set("display", "flex").set("justify-content", "space-between").set("align-items", "center");
        Span l = new Span(label);
        l.getStyle().set("color", "#64748B");
        Span v = new Span(value);
        v.getStyle().set("font-weight", "700").set("color", "#0F172A");
        row.add(l, v);
        return row;
    }

    private void resetPOS() {
        cartItems.clear();
        orderNotes = "";
        discountPercentage = 0;
        discountNominal = BigDecimal.ZERO;
        discountLabel = "Tanpa Diskon";
        taxPercentage = 0;
        taxLabel = "Tanpa Pajak";
        customerComboBox.clear();
        renderCartItems();
        updateQuickActionButtons();
    }

    private void showNotification(String message, NotificationVariant variant) {
        Notification notif = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notif.addThemeVariants(variant);
    }
}
