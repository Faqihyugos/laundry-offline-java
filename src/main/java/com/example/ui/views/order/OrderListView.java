package com.example.ui.views.order;

import com.example.domain.Order;
import com.example.domain.OrderStatus;
import com.example.service.CustomerService;
import com.example.service.OrderService;
import com.example.service.ReceiptService;
import com.example.ui.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Route(value = "orders", layout = MainLayout.class)
@PageTitle("Daftar Order | Laundry POS")
@RolesAllowed({"OWNER", "CASHIER"})
public class OrderListView extends VerticalLayout implements BeforeEnterObserver {

    private final OrderService orderService;
    private final Grid<Order> grid = new Grid<>(Order.class, false);
    private final Div mobileCardContainer = new Div();
    private final TextField searchField = new TextField();
    private final Tabs statusTabs = new Tabs();
    private final Map<Tab, OrderStatus> tabStatusMap = new HashMap<>();

    private OrderStatus currentStatusFilter = null; // null means All

    public OrderListView(OrderService orderService) {
        this.orderService = orderService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("max-width", "1280px").set("margin", "0 auto");

        createHeader();
        createTabs();
        createListComponents();
        refreshGrid();
    }

    private void createHeader() {
        H2 title = new H2("Manajemen Antrian Order Laundry");
        title.getStyle().set("margin", "0").set("color", "#7B2D8E").set("font-weight", "800");

        Paragraph subtitle = new Paragraph("Pantau status cucian dari proses terima sampai pengambilan oleh pelanggan.");
        subtitle.getStyle().set("margin", "4px 0 0.5rem 0").set("color", "#6C667A").set("font-size", "0.9rem");

        searchField.setPlaceholder("Cari No. Invoice, nama pelanggan, atau telepon...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setWidth("380px");
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setValueChangeTimeout(300);
        searchField.addValueChangeListener(e -> refreshGrid());

        Button newOrderBtn = new Button("Order Baru", VaadinIcon.PLUS.create(),
                e -> UI.getCurrent().navigate(CreateOrderView.class));
        newOrderBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newOrderBtn.getStyle().set("background-color", "#7B2D8E");

        Div toolbar = new Div();
        toolbar.addClassName("responsive-toolbar");
        toolbar.add(searchField, newOrderBtn);

        add(title, subtitle, toolbar);
    }

    private void createTabs() {
        Tab tabAll = new Tab("Semua Order");
        tabStatusMap.put(tabAll, null);

        Tab tabPending = new Tab("Pending");
        tabStatusMap.put(tabPending, OrderStatus.PENDING);

        Tab tabProses = new Tab("Proses");
        tabStatusMap.put(tabProses, OrderStatus.PROSES);

        Tab tabSelesai = new Tab("Selesai (Siap Ambil)");
        tabStatusMap.put(tabSelesai, OrderStatus.SELESAI);

        Tab tabDiambil = new Tab("Diambil");
        tabStatusMap.put(tabDiambil, OrderStatus.DIAMBIL);

        statusTabs.add(tabAll, tabPending, tabProses, tabSelesai, tabDiambil);
        statusTabs.setSelectedTab(tabAll);
        statusTabs.addClassName("scrollable-tabs");

        statusTabs.addSelectedChangeListener(e -> {
            currentStatusFilter = tabStatusMap.get(e.getSelectedTab());
            refreshGrid();
        });

        add(statusTabs);
    }

    private void createListComponents() {
        // Desktop Grid
        grid.setSizeFull();
        grid.addClassName("desktop-only");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        grid.addColumn(Order::getInvoiceNumber).setHeader("No. Invoice").setSortable(true).setAutoWidth(true);
        grid.addColumn(o -> o.getCustomer() != null ? o.getCustomer().getName() : "-").setHeader("Pelanggan").setAutoWidth(true);
        grid.addColumn(o -> o.getOrderDate().format(dtf)).setHeader("Tgl Masuk").setAutoWidth(true);
        grid.addColumn(o -> o.getEstimatedCompletionDate() != null ? o.getEstimatedCompletionDate().format(dtf) : "-")
                .setHeader("Estimasi Selesai").setAutoWidth(true);
        grid.addColumn(o -> o.getItems() != null ? o.getItems().size() + " item" : "0").setHeader("Item").setAutoWidth(true);
        grid.addColumn(o -> ReceiptService.formatRupiah(o.getTotalAmount())).setHeader("Total").setAutoWidth(true);

        grid.addComponentColumn(order -> {
            Span badge = new Span(order.getStatus().getDisplayName());
            badge.addClassName("status-badge");
            badge.addClassName(order.getStatus().getBadgeClass());
            return badge;
        }).setHeader("Status Cucian").setAutoWidth(true);

        grid.addComponentColumn(order -> {
            Span badge = new Span(order.getPaymentStatus().getDisplayName());
            badge.addClassName("status-badge");
            badge.addClassName(order.getPaymentStatus().getBadgeClass());
            return badge;
        }).setHeader("Status Bayar").setAutoWidth(true);

        grid.addComponentColumn(order -> {
            Button detailBtn = new Button("Detail & Proses", VaadinIcon.ARROW_RIGHT.create(),
                    e -> UI.getCurrent().navigate(OrderDetailView.class, order.getId()));
            detailBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            detailBtn.getStyle().set("background-color", "#7B2D8E");
            return detailBtn;
        }).setHeader("Aksi").setAutoWidth(true);

        // Mobile Card Container
        mobileCardContainer.addClassName("mobile-only");
        mobileCardContainer.setWidthFull();

        add(grid, mobileCardContainer);
    }

    private void refreshGrid() {
        String query = searchField.getValue();
        List<Order> orders;
        if (currentStatusFilter == null) {
            orders = orderService.search(query);
        } else {
            orders = orderService.searchByStatus(currentStatusFilter, query);
        }

        grid.setItems(orders);
        renderMobileCards(orders);
    }

    private void renderMobileCards(List<Order> orders) {
        mobileCardContainer.removeAll();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        if (orders.isEmpty()) {
            Div emptyMsg = new Div();
            emptyMsg.setText("Tidak ada pesanan laundry yang cocok dengan filter.");
            emptyMsg.getStyle().set("color", "#9C96A8").set("padding", "2rem").set("text-align", "center");
            mobileCardContainer.add(emptyMsg);
            return;
        }

        for (Order order : orders) {
            Div card = new Div();
            card.addClassName("mobile-order-card");

            // Header row: Invoice + Status Badge
            Div row1 = new Div();
            row1.addClassName("mobile-order-card-header");
            Span inv = new Span(order.getInvoiceNumber());
            inv.addClassName("mobile-order-inv");

            Span statusBadge = new Span(order.getStatus().getDisplayName());
            statusBadge.addClassNames("status-badge", order.getStatus().getBadgeClass());
            row1.add(inv, statusBadge);

            // Row 2: Customer + Payment Badge
            Div row2 = new Div();
            row2.getStyle().set("display", "flex").set("justify-content", "space-between").set("align-items", "center");
            Span cust = new Span("👤 " + (order.getCustomer() != null ? order.getCustomer().getName() : "-"));
            cust.addClassName("mobile-order-customer");

            Span payBadge = new Span(order.getPaymentStatus().getDisplayName());
            payBadge.addClassNames("status-badge", order.getPaymentStatus().getBadgeClass());
            row2.add(cust, payBadge);

            // Row 3: Meta (Date & Items)
            Div row3 = new Div();
            row3.addClassName("mobile-order-meta");
            Span dateSpan = new Span("Masuk: " + order.getOrderDate().format(dtf));
            Span itemSpan = new Span((order.getItems() != null ? order.getItems().size() : 0) + " item");
            row3.add(dateSpan, itemSpan);

            // Row 4: Total & Action
            Div row4 = new Div();
            row4.getStyle().set("display", "flex").set("justify-content", "space-between").set("align-items", "center").set("margin-top", "4px");
            Span total = new Span(ReceiptService.formatRupiah(order.getTotalAmount()));
            total.addClassName("mobile-order-total");

            HorizontalLayout btnGroup = new HorizontalLayout();
            btnGroup.setSpacing(true);

            if (order.getCustomer() != null && order.getCustomer().getPhone() != null) {
                String waUrl = CustomerService.buildWhatsAppUrl(order.getCustomer().getPhone(),
                        "Halo " + order.getCustomer().getName() + ", info status cucian No: " + order.getInvoiceNumber());
                Anchor waLink = new Anchor(waUrl, "");
                waLink.setTarget("_blank");
                Button waBtn = new Button(VaadinIcon.CHAT.create());
                waBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
                waLink.add(waBtn);
                btnGroup.add(waLink);
            }

            Button detailBtn = new Button("Detail & Proses", VaadinIcon.ARROW_RIGHT.create(), e -> {
                UI.getCurrent().navigate(OrderDetailView.class, order.getId());
            });
            detailBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            detailBtn.getStyle().set("background-color", "#7B2D8E");
            btnGroup.add(detailBtn);

            row4.add(total, btnGroup);

            card.add(row1, row2, row3, row4);
            mobileCardContainer.add(card);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters().containsKey("status")) {
            String statusParam = event.getLocation().getQueryParameters().getParameters().get("status").get(0);
            try {
                OrderStatus target = OrderStatus.valueOf(statusParam.toUpperCase());
                for (Map.Entry<Tab, OrderStatus> entry : tabStatusMap.entrySet()) {
                    if (entry.getValue() == target) {
                        statusTabs.setSelectedTab(entry.getKey());
                        currentStatusFilter = target;
                        refreshGrid();
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
}
