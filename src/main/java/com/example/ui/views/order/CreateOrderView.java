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
@PageTitle("POS Kasir | CWB Laundry POS")
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

    // Quick Action Bar States (Figma node 19:5078)
    private String orderNotes = "";
    private int discountPercentage = 0;
    private BigDecimal discountNominal = BigDecimal.ZERO;
    private String discountLabel = "Tanpa Diskon";
    private int taxPercentage = 0;
    private String taxLabel = "Tanpa Pajak";
    private BigDecimal shippingFee = BigDecimal.ZERO;

    // 4 Square Quick Action Buttons
    private final Div ongkirBtn = new Div();
    private final Span ongkirLabelSpan = new Span("Ongkir");

    private final Div discountBtn = new Div();
    private final Span discountLabelSpan = new Span("Diskon");

    private final Div taxBtn = new Div();
    private final Span taxLabelSpan = new Span("Pajak");

    private final Div customerBtn = new Div();
    private final Span customerLabelSpan = new Span("Pelanggan");

    // Financial Summary Elements
    private final Span subtotalAmountSpan = new Span("Rp 0");
    private final Span shippingAmountSpan = new Span("0");
    private final Div discountSummaryRow = new Div();
    private final Span discountAmountSpan = new Span("Rp. 0");
    private final Div taxSummaryRow = new Div();
    private final Span taxAmountSpan = new Span("0 %");
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
        getStyle().set("background-color", "#FAFAFA")
                .set("padding", "20px 24px");

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
        panel.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "16px");

        // Header with title and search field (Figma node 10:7139)
        Div headerRow = new Div();
        headerRow.getStyle().set("display", "flex").set("justify-content", "space-between").set("align-items", "center").set("flex-wrap", "wrap").set("gap", "16px");

        Div titleGroup = new Div();
        String laundryName = settingService.getSetting(SettingService.KEY_LAUNDRY_NAME, "CWB Laundry");
        H1 title = new H1(laundryName + " POS");
        title.getStyle().set("margin", "0").set("color", "#3949ab").set("font-size", "28px").set("font-weight", "600").set("line-height", "1.2");

        LocalDate today = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", new Locale("id", "ID"));
        Paragraph sub = new Paragraph(today.format(dtf));
        sub.getStyle().set("font-size", "16px").set("color", "#7890cd").set("margin", "4px 0 0 0").set("font-weight", "400");
        titleGroup.add(title, sub);

        searchServiceField.setPlaceholder("Search for food, coffee, etc..");
        searchServiceField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchServiceField.setClearButtonVisible(true);
        searchServiceField.addClassName("figma-search-field");
        searchServiceField.getStyle().set("width", "260px");
        searchServiceField.setValueChangeMode(ValueChangeMode.LAZY);
        searchServiceField.addValueChangeListener(e -> refreshCatalogGrid());

        headerRow.add(titleGroup, searchServiceField);

        // Category Filter Tabs with underline indicator (Figma node 10:7145)
        categoryBar.addClassName("figma-category-bar");
        renderCategoryTabs();

        // Service Catalog Cards Grid (Figma node 10:7160)
        serviceGridContainer.addClassName("figma-catalog-grid");
        refreshCatalogGrid();

        panel.add(headerRow, categoryBar, serviceGridContainer);
        return panel;
    }

    private void renderCategoryTabs() {
        categoryBar.removeAll();
        String[] categories = {"Semua", "Kiloan", "Satuan", "Express", "Bedcover"};

        Div tabsContainer = new Div();
        tabsContainer.getStyle().set("display", "flex").set("gap", "32px").set("align-items", "center");

        for (String cat : categories) {
            Div tabItem = new Div();
            tabItem.addClassName("figma-tab-item");

            Span tabText = new Span(cat);
            tabText.addClassName("figma-tab-text");

            Div indicator = new Div();
            indicator.addClassName("figma-tab-indicator");

            if (cat.equalsIgnoreCase(selectedCategory)) {
                tabItem.addClassName("active");
            }

            tabItem.add(tabText, indicator);
            tabItem.addClickListener(e -> {
                selectedCategory = cat;
                renderCategoryTabs();
                refreshCatalogGrid();
            });
            tabsContainer.add(tabItem);
        }

        categoryBar.add(tabsContainer);
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
            emptyMsg.setText("Tidak ada paket layanan yang sesuai.");
            emptyMsg.getStyle().set("color", "#ABBBC2").set("padding", "3rem 1rem").set("text-align", "center").set("grid-column", "1 / -1");
            serviceGridContainer.add(emptyMsg);
            return;
        }

        for (LaundryService service : services) {
            Div card = createServiceCard(service);
            serviceGridContainer.add(card);
        }
    }

    private Div createServiceCard(LaundryService service) {
        // Figma card dimensions: width: 192px, height: 259px, rounded 16px, border: 1px solid #c7d0eb
        Div card = new Div();
        card.addClassName("figma-product-card");

        // Top right: circular basket button (Figma Ellipse 120, size 35px)
        Div basketBtn = new Div();
        basketBtn.addClassName("figma-card-basket-btn");
        Icon basketIcon = VaadinIcon.CART.create();
        basketIcon.setSize("16px");
        basketIcon.setColor("#FFFFFF");
        basketBtn.add(basketIcon);
        basketBtn.getElement().setAttribute("title", "Tambahkan ke Tiket");
        basketBtn.addClickListener(e -> addItemToCart(service));

        // Center: Circular service image/illustration (Figma Group 975, 132px x 132px)
        Div circleContainer = new Div();
        circleContainer.addClassName("figma-card-circle-outer");

        Div circleInner = new Div();
        circleInner.addClassName("figma-card-circle-inner");

        Icon serviceIcon = getServiceIcon(service.getName(), service.getUnit());
        serviceIcon.setSize("36px");
        serviceIcon.setColor("#3949ab");
        circleInner.add(serviceIcon);
        circleContainer.add(circleInner);

        // Bottom area:
        // Title: Figma Dish Name (14px, medium #3949ab)
        Div name = new Div();
        name.addClassName("figma-card-title");
        name.setText(service.getName());

        // Category & Price row (14px #abbbc2 & 14px bold #3949ab)
        Div bottomRow = new Div();
        bottomRow.addClassName("figma-card-bottom-row");

        Span unitSpan = new Span(service.getUnit() == ServiceUnit.KG ? "Kiloan" : "Satuan");
        unitSpan.addClassName("figma-card-unit");

        Span priceSpan = new Span(ReceiptService.formatRupiah(service.getPrice()));
        priceSpan.addClassName("figma-card-price");

        bottomRow.add(unitSpan, priceSpan);

        card.add(basketBtn, circleContainer, name, bottomRow);
        card.addClickListener(e -> addItemToCart(service));
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
        // Figma right rail: Orders #1 (width: 409px, rounded 10px, white bg)
        Div panel = new Div();
        panel.addClassName("figma-ticket-panel");

        // 1. Header: Order Title & Type Selector Tabs (Figma node 10:7278, 10:7398)
        Div header = new Div();
        header.addClassName("figma-ticket-header");

        Div titleRow = new Div();
        titleRow.addClassName("figma-ticket-title-row");
        H2 title = new H2("Orders #1");
        title.getStyle().set("margin", "0").set("color", "#3949ab").set("font-size", "20px").set("font-weight", "600");
        titleRow.add(title);

        // Selector Tabs: [ Drop Off ] [ Express ] [ Delivery ] (Figma node 10:7398)
        Div selectorTabs = new Div();
        selectorTabs.addClassName("figma-order-selector");

        Span tab1 = new Span("Drop Off");
        tab1.addClassName("figma-order-tab");
        tab1.addClassName("active");

        Span tab2 = new Span("Express");
        tab2.addClassName("figma-order-tab");

        Span tab3 = new Span("Delivery");
        tab3.addClassName("figma-order-tab");

        selectorTabs.add(tab1, tab2, tab3);

        // Customer Selection
        customerComboBox.setWidthFull();
        customerComboBox.setPlaceholder("Pilih / Cari Pelanggan...");
        customerComboBox.setItemLabelGenerator(c -> c.getName() + " (" + c.getPhone() + ")");
        customerComboBox.addValueChangeListener(e -> updateQuickActionButtons());
        refreshCustomerCombo();

        Button addCustomerBtn = new Button(VaadinIcon.PLUS.create(), e -> openCustomerModal());
        addCustomerBtn.addClassName("figma-btn-primary");
        addCustomerBtn.setTooltipText("Tambah Pelanggan Baru");

        HorizontalLayout customerRow = new HorizontalLayout(customerComboBox, addCustomerBtn);
        customerRow.setWidthFull();
        customerRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        customerRow.getStyle().set("gap", "8px").set("margin-top", "6px");

        header.add(titleRow, selectorTabs, customerRow);

        // 2. Table Column Headers: Item | Qty | Price (Figma node 10:7279)
        Div columnsHeader = new Div();
        columnsHeader.addClassName("figma-ticket-cols-header");

        Span colItem = new Span("Item");
        colItem.addClassName("col-item");
        Span colQty = new Span("Qty");
        colQty.addClassName("col-qty");
        Span colPrice = new Span("Price");
        colPrice.addClassName("col-price");

        columnsHeader.add(colItem, colQty, colPrice);

        // 3. Middle: Scrollable Cart Items List (Figma node 10:7289)
        cartItemsContainer.addClassName("figma-ticket-items-scroll");

        // 4. Footer: Quick Action 4 Square Buttons, Financial Breakdown, & CTA
        Div footer = new Div();
        footer.addClassName("figma-ticket-footer");

        // Quick Action Bar (Ongkir, Diskon, Pajak, Pelanggan - Figma node 19:5078)
        Div quickActionBar = createQuickActionBar();

        // Financial Summary Box (Figma node 19:5065)
        Div summaryBox = createSummaryBox();

        renderCartItems();

        estDatePicker.setValue(LocalDateTime.now().plusHours(24));
        estDatePicker.setVisible(false);

        Button checkoutBtn = new Button("Lanjutkan Pembayaran", e -> openPaymentModal());
        checkoutBtn.addClassName("figma-checkout-btn");

        footer.add(quickActionBar, summaryBox, checkoutBtn);

        panel.add(header, columnsHeader, cartItemsContainer, footer);
        return panel;
    }

    private Div createQuickActionBar() {
        // Figma node 19:5078: 4 square buttons (48px x 48px) with label underneath
        Div bar = new Div();
        bar.addClassName("figma-quick-actions");

        setupActionBtn(ongkirBtn, VaadinIcon.TRUCK, ongkirLabelSpan, e -> openOngkirModal());
        setupActionBtn(discountBtn, VaadinIcon.TAG, discountLabelSpan, e -> openDiscountModal());
        setupActionBtn(taxBtn, VaadinIcon.INVOICE, taxLabelSpan, e -> openTaxModal());
        setupActionBtn(customerBtn, VaadinIcon.USER, customerLabelSpan, e -> openCustomerModal());

        bar.add(ongkirBtn, discountBtn, taxBtn, customerBtn);
        updateQuickActionButtons();
        return bar;
    }

    private void setupActionBtn(Div wrapper, VaadinIcon iconType, Span labelSpan, com.vaadin.flow.component.ComponentEventListener<com.vaadin.flow.component.ClickEvent<Div>> listener) {
        wrapper.addClassName("figma-action-wrapper");
        wrapper.removeAll();

        Div squareBox = new Div();
        squareBox.addClassName("figma-action-square");
        Icon icon = iconType.create();
        icon.setSize("20px");
        icon.setColor("#3949ab");
        squareBox.add(icon);

        labelSpan.addClassName("figma-action-label");

        wrapper.add(squareBox, labelSpan);
        wrapper.addClickListener(listener);
    }

    private void updateQuickActionButtons() {
        // 1. Ongkir state
        if (shippingFee.compareTo(BigDecimal.ZERO) > 0) {
            ongkirBtn.addClassName("active");
            ongkirLabelSpan.setText(ReceiptService.formatRupiah(shippingFee));
        } else {
            ongkirBtn.removeClassName("active");
            ongkirLabelSpan.setText("Ongkir");
        }

        // 2. Discount state
        if (discountPercentage > 0) {
            discountBtn.addClassName("active");
            discountLabelSpan.setText(discountPercentage + "%");
        } else if (discountNominal.compareTo(BigDecimal.ZERO) > 0) {
            discountBtn.addClassName("active");
            discountLabelSpan.setText("Diskon");
        } else {
            discountBtn.removeClassName("active");
            discountLabelSpan.setText("Diskon");
        }

        // 3. Tax state
        if (taxPercentage > 0) {
            taxBtn.addClassName("active");
            taxLabelSpan.setText("PPN " + taxPercentage + "%");
        } else {
            taxBtn.removeClassName("active");
            taxLabelSpan.setText("Pajak");
        }

        // 4. Customer state
        Customer cust = customerComboBox.getValue();
        if (cust != null) {
            customerBtn.addClassName("active");
            String firstName = cust.getName().split(" ")[0];
            customerLabelSpan.setText(firstName);
        } else {
            customerBtn.removeClassName("active");
            customerLabelSpan.setText("Pelanggan");
        }
    }

    private Div createSummaryBox() {
        // Figma node 19:5065: Ongkir, Pajak, Diskon, Sub total
        Div box = new Div();
        box.addClassName("figma-summary-box");

        // Ongkir
        Div ongkirRow = new Div();
        ongkirRow.addClassName("figma-summary-row");
        Span ongkirLbl = new Span("Ongkir");
        ongkirRow.add(ongkirLbl, shippingAmountSpan);

        // Pajak
        taxSummaryRow.addClassName("figma-summary-row");
        Span taxLbl = new Span("Pajak");
        taxSummaryRow.add(taxLbl, taxAmountSpan);

        // Diskon
        discountSummaryRow.addClassName("figma-summary-row");
        Span discLbl = new Span("Diskon");
        discountSummaryRow.add(discLbl, discountAmountSpan);

        // Sub total
        Div subtotalRow = new Div();
        subtotalRow.addClassName("figma-summary-row");
        subtotalRow.addClassName("bold");
        Span subLbl = new Span("Sub total");
        subtotalRow.add(subLbl, subtotalAmountSpan);

        box.add(ongkirRow, taxSummaryRow, discountSummaryRow, subtotalRow);
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
            emptyBox.getStyle().set("text-align", "center").set("padding", "3rem 1rem").set("color", "#ABBBC2");
            Icon icon = VaadinIcon.CART.create();
            icon.setSize("36px");
            icon.setColor("#C8D1E1");
            icon.getStyle().set("display", "block").set("margin", "0 auto 8px auto");
            Span txt = new Span("Belum ada item pesanan");
            txt.getStyle().set("font-size", "14px").set("font-weight", "600").set("display", "block");
            Span subTxt = new Span("Pilih layanan di sebelah kiri");
            subTxt.getStyle().set("font-size", "12px").set("color", "#ABBBC2");
            emptyBox.add(icon, txt, subTxt);
            cartItemsContainer.add(emptyBox);
            recalculateCartTotals();
            return;
        }

        for (int i = 0; i < cartItems.size(); i++) {
            final int index = i;
            OrderItem item = cartItems.get(i);

            // Figma cart item container
            Div itemCard = new Div();
            itemCard.addClassName("figma-cart-item-card");

            // --- Row 1: Item Info | Qty | Price (Figma node 10:7304) ---
            Div mainRow = new Div();
            mainRow.addClassName("figma-cart-item-main-row");

            // Left: thumbnail + name + rate
            Div itemInfo = new Div();
            itemInfo.addClassName("figma-cart-item-info");

            Div thumb = new Div();
            thumb.addClassName("figma-cart-thumb");
            Icon thumbIcon = getServiceIcon(item.getServiceName(), item.getUnit());
            thumbIcon.setSize("20px");
            thumbIcon.setColor("#3949ab");
            thumb.add(thumbIcon);

            Div titles = new Div();
            titles.addClassName("figma-cart-item-titles");
            Span nameSpan = new Span(item.getServiceName());
            nameSpan.addClassName("name");
            Span rateSpan = new Span(ReceiptService.formatRupiah(item.getUnitPrice()));
            rateSpan.addClassName("rate");
            titles.add(nameSpan, rateSpan);
            itemInfo.add(thumb, titles);

            // Middle: Qty Box (Figma node 10:7285: 48px x 48px with border #3949ab)
            Div qtyBox = new Div();
            qtyBox.addClassName("figma-cart-qty-box");

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
            minusBtn.addClassName("stepper-sub-btn");

            Span qtyVal = new Span(ReceiptService.formatQuantity(item.getQuantity()));
            qtyVal.addClassName("qty-val");

            Button plusBtn = new Button("+", e -> {
                item.setQuantity(item.getQuantity().add(BigDecimal.ONE));
                item.setSubtotal(item.getUnitPrice().multiply(item.getQuantity()));
                renderCartItems();
            });
            plusBtn.addClassName("stepper-sub-btn");

            qtyBox.add(minusBtn, qtyVal, plusBtn);

            // Right: Price (Figma node 10:7320: 16px medium #3949ab)
            Span linePrice = new Span(ReceiptService.formatRupiah(item.getSubtotal()));
            linePrice.addClassName("figma-cart-line-price");

            mainRow.add(itemInfo, qtyBox, linePrice);

            // --- Row 2: Note input + Delete Button (Figma node 10:7319, 10:7318) ---
            Div noteRow = new Div();
            noteRow.addClassName("figma-cart-note-row");

            TextField noteField = new TextField();
            noteField.setPlaceholder("Catatan pesanan");
            noteField.addClassName("figma-cart-note-input");
            noteField.setValue(item.getNotes() != null ? item.getNotes() : "");
            noteField.setValueChangeMode(ValueChangeMode.LAZY);
            noteField.addValueChangeListener(e -> item.setNotes(e.getValue()));

            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                cartItems.remove(index);
                renderCartItems();
            });
            deleteBtn.addClassName("figma-cart-trash-btn");
            deleteBtn.setTooltipText("Hapus Item");

            noteRow.add(noteField, deleteBtn);

            itemCard.add(mainRow, noteRow);
            cartItemsContainer.add(itemCard);
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
        return afterDiscount.add(tax).add(shippingFee);
    }

    private void recalculateCartTotals() {
        BigDecimal subtotal = getCalculatedSubtotal();
        BigDecimal discount = getCalculatedDiscount(subtotal);
        BigDecimal tax = getCalculatedTax(subtotal.subtract(discount).max(BigDecimal.ZERO));
        BigDecimal grandTotal = getFinalTotal();

        subtotalAmountSpan.setText(ReceiptService.formatRupiah(grandTotal));
        shippingAmountSpan.setText(shippingFee.compareTo(BigDecimal.ZERO) > 0 ? ReceiptService.formatRupiah(shippingFee) : "0");

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            discountAmountSpan.setText("- " + ReceiptService.formatRupiah(discount));
        } else {
            discountAmountSpan.setText("Rp. 0");
        }

        if (taxPercentage > 0) {
            taxAmountSpan.setText(taxPercentage + " %");
        } else {
            taxAmountSpan.setText("0 %");
        }

        updateQuickActionButtons();
    }

    // ==========================================
    // Modals: Ongkir, Notes, Discount, Tax, Customer
    // ==========================================

    private void openOngkirModal() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Biaya Pengantaran (Ongkir)");
        dialog.setWidth("380px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        BigDecimalField feeField = new BigDecimalField("Nominal Ongkir (Rp)");
        feeField.setWidthFull();
        feeField.setValue(shippingFee);

        HorizontalLayout quickChips = new HorizontalLayout();
        Button freeBtn = new Button("Gratis (0)", e -> feeField.setValue(BigDecimal.ZERO));
        Button k10Btn = new Button("10k", e -> feeField.setValue(BigDecimal.valueOf(10000)));
        Button k15Btn = new Button("15k", e -> feeField.setValue(BigDecimal.valueOf(15000)));
        quickChips.add(freeBtn, k10Btn, k15Btn);

        layout.add(feeField, quickChips);

        Button saveBtn = new Button("Simpan", e -> {
            shippingFee = feeField.getValue() != null ? feeField.getValue() : BigDecimal.ZERO;
            recalculateCartTotals();
            dialog.close();
        });
        saveBtn.addClassName("figma-btn-primary");

        Button cancelBtn = new Button("Batal", e -> dialog.close());
        dialog.add(layout);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void openNotesModal() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Catatan Khusus Pesanan");
        dialog.setWidth("420px");

        TextArea notesArea = new TextArea();
        notesArea.setWidthFull();
        notesArea.setPlaceholder("Contoh: Jangan campur baju putih dengan luntur, deterjen sensitive...");
        notesArea.setValue(orderNotes != null ? orderNotes : "");
        notesArea.setMinHeight("120px");

        Button saveBtn = new Button("Simpan Catatan", e -> {
            orderNotes = notesArea.getValue() != null ? notesArea.getValue().trim() : "";
            updateQuickActionButtons();
            dialog.close();
        });
        saveBtn.addClassName("figma-btn-primary");

        Button cancelBtn = new Button("Batal", e -> dialog.close());
        dialog.add(notesArea);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void openDiscountModal() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Pilih / Terapkan Diskon");
        dialog.setWidth("420px");

        RadioButtonGroup<String> discountGroup = new RadioButtonGroup<>();
        discountGroup.setItems("Tanpa Diskon", "Member 10%", "Promo 15%", "Potongan Rp 5.000", "Potongan Rp 10.000");
        discountGroup.setValue(discountLabel);

        Button saveBtn = new Button("Terapkan Diskon", e -> {
            String selected = discountGroup.getValue();
            discountLabel = selected;
            discountPercentage = 0;
            discountNominal = BigDecimal.ZERO;

            if ("Member 10%".equals(selected)) {
                discountPercentage = 10;
            } else if ("Promo 15%".equals(selected)) {
                discountPercentage = 15;
            } else if ("Potongan Rp 5.000".equals(selected)) {
                discountNominal = BigDecimal.valueOf(5000);
            } else if ("Potongan Rp 10.000".equals(selected)) {
                discountNominal = BigDecimal.valueOf(10000);
            }

            recalculateCartTotals();
            dialog.close();
        });
        saveBtn.addClassName("figma-btn-primary");

        Button cancelBtn = new Button("Batal", e -> dialog.close());
        dialog.add(discountGroup);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void openTaxModal() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Pengaturan Pajak (PPN)");
        dialog.setWidth("380px");

        RadioButtonGroup<String> taxGroup = new RadioButtonGroup<>();
        taxGroup.setItems("Tanpa Pajak (0%)", "PPN 11%");
        taxGroup.setValue(taxPercentage == 11 ? "PPN 11%" : "Tanpa Pajak (0%)");

        Button saveBtn = new Button("Terapkan Pajak", e -> {
            String val = taxGroup.getValue();
            if ("PPN 11%".equals(val)) {
                taxPercentage = 11;
                taxLabel = "PPN 11%";
            } else {
                taxPercentage = 0;
                taxLabel = "Bebas Pajak";
            }
            recalculateCartTotals();
            dialog.close();
        });
        saveBtn.addClassName("figma-btn-primary");

        Button cancelBtn = new Button("Batal", e -> dialog.close());
        dialog.add(taxGroup);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void openCustomerModal() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Pilih atau Tambah Pelanggan");
        dialog.setWidth("480px");

        VerticalLayout form = new VerticalLayout();
        form.setPadding(false);
        form.setSpacing(true);

        ComboBox<Customer> existingCustomerCombo = new ComboBox<>("Cari Pelanggan Terdaftar");
        existingCustomerCombo.setWidthFull();
        existingCustomerCombo.setItems(customerService.findAll());
        existingCustomerCombo.setItemLabelGenerator(c -> c.getName() + " (" + c.getPhone() + ")");
        if (customerComboBox.getValue() != null) {
            existingCustomerCombo.setValue(customerComboBox.getValue());
        }

        Span orDivider = new Span("— Atau Daftarkan Pelanggan Baru —");
        orDivider.getStyle().set("color", "#7890cd").set("font-size", "0.8rem").set("text-align", "center").set("display", "block").set("margin", "6px 0");

        TextField nameField = new TextField("Nama Lengkap Pelanggan");
        nameField.setWidthFull();

        TextField phoneField = new TextField("Nomor WhatsApp / Telepon");
        phoneField.setWidthFull();
        phoneField.setPlaceholder("08xxxxxxxxxx");

        TextArea addressField = new TextArea("Alamat (Opsional)");
        addressField.setWidthFull();

        form.add(existingCustomerCombo, orDivider, nameField, phoneField, addressField);

        Button saveBtn = new Button("Pilih / Simpan", e -> {
            if (existingCustomerCombo.getValue() != null && nameField.isEmpty()) {
                customerComboBox.setValue(existingCustomerCombo.getValue());
                updateQuickActionButtons();
                dialog.close();
                return;
            }

            if (!nameField.isEmpty() && !phoneField.isEmpty()) {
                try {
                    Customer newCustomer = customerService.createCustomer(nameField.getValue().trim(), phoneField.getValue().trim(), addressField.getValue());
                    refreshCustomerCombo();
                    customerComboBox.setValue(newCustomer);
                    updateQuickActionButtons();
                    dialog.close();
                    showNotification("Pelanggan " + newCustomer.getName() + " berhasil didaftarkan", NotificationVariant.LUMO_SUCCESS);
                    return;
                } catch (Exception ex) {
                    showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
                    return;
                }
            }

            showNotification("Pilih pelanggan atau lengkapi formulir pelanggan baru", NotificationVariant.LUMO_WARNING);
        });
        saveBtn.addClassName("figma-btn-primary");

        Button cancelBtn = new Button("Batal", e -> dialog.close());
        dialog.add(form);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    // ==========================================
    // Payment Modal: Split Confirmation & Payment (Figma node 15:5977)
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
        dialog.setWidth("830px");
        dialog.setMaxWidth("95vw");

        // Figma 2-Column Layout: Left = Konfirmasi Orders #1, Right = Pembayaran (Metode QRIS & Cash)
        Div splitContainer = new Div();
        splitContainer.addClassName("figma-split-checkout");

        // --- LEFT COLUMN: Konfirmasi Orders #1 (Figma node 15:4226) ---
        Div leftCol = new Div();
        leftCol.addClassName("figma-checkout-left-col");

        H2 leftTitle = new H2("Konfirmasi");
        leftTitle.addClassName("title");
        Span leftSub = new Span("Orders #1");
        leftSub.addClassName("sub");
        leftCol.add(leftTitle, leftSub);

        // Column headers: Item | Qty | Price
        Div itemHeaders = new Div();
        itemHeaders.addClassName("figma-ticket-cols-header");
        itemHeaders.add(new Span("Item"), new Span("Qty"), new Span("Price"));
        leftCol.add(itemHeaders);

        Div leftItemsList = new Div();
        leftItemsList.addClassName("figma-checkout-items-list");

        for (OrderItem item : cartItems) {
            Div r = new Div();
            r.addClassName("figma-checkout-item-row");

            Div leftInfo = new Div();
            Span n = new Span(item.getServiceName());
            n.getStyle().set("font-weight", "600").set("color", "#3949ab").set("display", "block");
            Span p = new Span(ReceiptService.formatRupiah(item.getUnitPrice()));
            p.getStyle().set("font-size", "12px").set("color", "#ABBBC2");
            leftInfo.add(n, p);

            Span q = new Span(ReceiptService.formatQuantity(item.getQuantity()));
            q.addClassName("qty-chip");

            Span pr = new Span(ReceiptService.formatRupiah(item.getSubtotal()));
            pr.getStyle().set("font-weight", "600").set("color", "#3949ab");

            r.add(leftInfo, q, pr);
            leftItemsList.add(r);
        }
        leftCol.add(leftItemsList);

        // --- RIGHT COLUMN: Pembayaran (Figma node 15:4217) ---
        Div rightCol = new Div();
        rightCol.addClassName("figma-checkout-right-col");

        H2 rightTitle = new H2("Pembayaran");
        rightTitle.addClassName("title");
        Paragraph rightSub = new Paragraph("2 opsi pembayaran tersedia");
        rightSub.addClassName("sub");

        // Payment Method Tabs (Figma node 15:4188: QRIS card & Cash card, 101px x 64px)
        Div methodTabs = new Div();
        methodTabs.addClassName("figma-payment-methods");

        Div qrisCard = new Div();
        qrisCard.addClassName("figma-method-card");
        qrisCard.addClassName("active");
        qrisCard.setText("QRIS");

        Div cashCard = new Div();
        cashCard.addClassName("figma-method-card");
        Icon walletIcon = VaadinIcon.MONEY.create();
        walletIcon.setSize("20px");
        walletIcon.setColor("#ABBBC2");
        Span cashText = new Span("Cash");
        cashCard.add(walletIcon, cashText);

        methodTabs.add(qrisCard, cashCard);

        // Content Area: QRIS View or Cash View
        Div qrisView = new Div();
        qrisView.addClassName("figma-method-view");

        String qrisPayload = "00020101021226600016ID.CO.QRIS.WWW011893600999000000000152045812530336054" +
                finalTotal.toPlainString() + "5802ID5914CWB LAUNDRY6007JAKARTA6304";
        Div qrSvg = QRSvgHelper.createQRISComponent(qrisPayload, 200);
        Paragraph qrNote = new Paragraph("Scan QRIS untuk menyelesaikan pembayaran");
        qrNote.getStyle().set("font-size", "12px").set("color", "#ABBBC2").set("text-align", "center");
        qrisView.add(qrSvg, qrNote);

        Div cashView = new Div();
        cashView.addClassName("figma-method-view");
        cashView.setVisible(false);

        BigDecimalField paidField = new BigDecimalField("Uang Diterima (Rp)");
        paidField.setWidthFull();
        paidField.setValue(finalTotal);
        paidField.setValueChangeMode(ValueChangeMode.EAGER);

        Div quickCashChips = new Div();
        quickCashChips.addClassName("figma-quick-cash");
        Button pasChip = new Button("Uang Pas", e -> paidField.setValue(finalTotal));
        Button k20Chip = new Button("20k", e -> paidField.setValue(BigDecimal.valueOf(20000)));
        Button k50Chip = new Button("50k", e -> paidField.setValue(BigDecimal.valueOf(50000)));
        Button k100Chip = new Button("100k", e -> paidField.setValue(BigDecimal.valueOf(100000)));
        quickCashChips.add(pasChip, k20Chip, k50Chip, k100Chip);

        Span changeDisplay = new Span("Kembalian: Rp 0");
        changeDisplay.getStyle().set("font-weight", "700").set("color", "#059669").set("font-size", "15px").set("margin-top", "8px").set("display", "block");

        paidField.addValueChangeListener(e -> {
            BigDecimal p = e.getValue() != null ? e.getValue() : BigDecimal.ZERO;
            if (p.compareTo(finalTotal) >= 0) {
                BigDecimal kembalian = p.subtract(finalTotal);
                changeDisplay.setText("Kembalian: " + ReceiptService.formatRupiah(kembalian));
                changeDisplay.getStyle().set("color", "#059669");
            } else {
                BigDecimal sisa = finalTotal.subtract(p);
                changeDisplay.setText("Sisa Tagihan (DP): " + ReceiptService.formatRupiah(sisa));
                changeDisplay.getStyle().set("color", "#D97706");
            }
        });

        cashView.add(paidField, quickCashChips, changeDisplay);

        // Toggle logic
        final boolean[] isQris = {true};
        qrisCard.addClickListener(e -> {
            isQris[0] = true;
            qrisCard.addClassName("active");
            cashCard.removeClassName("active");
            qrisView.setVisible(true);
            cashView.setVisible(false);
        });

        cashCard.addClickListener(e -> {
            isQris[0] = false;
            cashCard.addClassName("active");
            qrisCard.removeClassName("active");
            qrisView.setVisible(false);
            cashView.setVisible(true);
        });

        // Bottom Actions: [ Batalkan ] (175px border) and [ Konfirmasi ] (175px solid #3949ab) (Figma node 15:4183)
        Div buttonRow = new Div();
        buttonRow.addClassName("figma-checkout-actions");

        Button cancelBtn = new Button("Batalkan", e -> dialog.close());
        cancelBtn.addClassName("figma-btn-cancel");

        Button confirmBtn = new Button("Konfirmasi", e -> {
            try {
                User cashier = authenticatedUser.get().orElse(null);
                LocalDateTime est = estDatePicker.getValue();
                BigDecimal discount = getCalculatedDiscount(getCalculatedSubtotal());
                BigDecimal tax = getCalculatedTax(getCalculatedSubtotal().subtract(discount));

                PaymentMethod method = isQris[0] ? PaymentMethod.TRANSFER : PaymentMethod.CASH;
                BigDecimal paid = isQris[0] ? finalTotal : (paidField.getValue() != null ? paidField.getValue() : BigDecimal.ZERO);

                Order saved = orderService.createOrder(customer, cashier, cartItems, est, method, paid, orderNotes, discount, tax);
                dialog.close();

                BigDecimal kembalian = paid.compareTo(finalTotal) >= 0 ? paid.subtract(finalTotal) : BigDecimal.ZERO;
                String methodLabel = isQris[0] ? "QRIS" : "Tunai";
                openSuccessStrukModal(saved, finalTotal, paid, kembalian, methodLabel);
            } catch (Exception ex) {
                showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        confirmBtn.addClassName("figma-btn-confirm");

        buttonRow.add(cancelBtn, confirmBtn);

        rightCol.add(rightTitle, rightSub, methodTabs, qrisView, cashView, buttonRow);

        splitContainer.add(leftCol, rightCol);
        dialog.add(splitContainer);
        dialog.open();
    }

    // ==========================================
    // Success Struk Modal (Figma node 20:5252 - Struk n Print)
    // ==========================================

    private void openSuccessStrukModal(Order order, BigDecimal total, BigDecimal paid, BigDecimal kembalian, String method) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");
        dialog.setMaxWidth("95vw");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);

        // Circular Success Badge (Figma size 80px)
        Div checkBadge = new Div();
        checkBadge.addClassName("figma-success-badge");
        Icon checkIcon = VaadinIcon.CHECK.create();
        checkIcon.setSize("40px");
        checkIcon.setColor("#FFFFFF");
        checkBadge.add(checkIcon);

        H3 successTitle = new H3("Pembayaran telah sukses dilakukan");
        successTitle.addClassName("figma-success-title");

        // Receipt Details with Dividers (Figma node 20:5235)
        Div receiptDetails = new Div();
        receiptDetails.addClassName("figma-receipt-details");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d MMMM, HH:mm", new Locale("id", "ID"));
        String timeStr = order.getOrderDate() != null ? order.getOrderDate().format(dtf) : LocalDateTime.now().format(dtf);

        receiptDetails.add(createReceiptDetailRow("METODE BAYAR", method));
        receiptDetails.add(createReceiptDetailRow("TOTAL TAGIHAN", ReceiptService.formatRupiah(total)));
        receiptDetails.add(createReceiptDetailRow("NOMINAL BAYAR", ReceiptService.formatRupiah(paid)));
        receiptDetails.add(createReceiptDetailRow("WAKTU PEMBAYARAN", timeStr));

        // Buttons (Figma node 19:4802): [ Batalkan / Selesai ] and [ Print ]
        Div btnRow = new Div();
        btnRow.addClassName("figma-receipt-actions");

        Button doneBtn = new Button("Selesai", e -> {
            resetPOS();
            dialog.close();
        });
        doneBtn.addClassName("figma-btn-cancel");

        Button printBtn = new Button("Print", e -> {
            ThermalReceiptDialog printDialog = new ThermalReceiptDialog(receiptService, settingService, order);
            printDialog.open();
        });
        printBtn.addClassName("figma-btn-confirm");

        Button waBtn = new Button("WhatsApp", VaadinIcon.CHAT.create(), e -> {
            String url = receiptService.generateWhatsAppUrl(order);
            UI.getCurrent().getPage().open(url, "_blank");
        });
        waBtn.addClassName("figma-btn-wa");

        btnRow.add(doneBtn, waBtn, printBtn);

        layout.add(checkBadge, successTitle, receiptDetails, btnRow);
        dialog.add(layout);
        dialog.open();
    }

    private Div createReceiptDetailRow(String label, String value) {
        Div row = new Div();
        row.addClassName("figma-receipt-row");

        Span l = new Span(label);
        l.addClassName("lbl");
        Span v = new Span(value);
        v.addClassName("val");

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
        shippingFee = BigDecimal.ZERO;
        customerComboBox.clear();
        renderCartItems();
    }

    private void showNotification(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(variant);
    }
}
