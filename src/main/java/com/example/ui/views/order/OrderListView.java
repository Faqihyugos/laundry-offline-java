package com.example.ui.views.order;

import com.example.domain.Order;
import com.example.domain.OrderStatus;
import com.example.service.OrderService;
import com.example.service.ReceiptService;
import com.example.ui.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
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
import java.util.Map;

@Route(value = "orders", layout = MainLayout.class)
@PageTitle("Daftar Order | Laundry POS")
@RolesAllowed({"OWNER", "CASHIER"})
public class OrderListView extends VerticalLayout implements BeforeEnterObserver {

    private final OrderService orderService;
    private final Grid<Order> grid = new Grid<>(Order.class, false);
    private final TextField searchField = new TextField();
    private final Tabs statusTabs = new Tabs();
    private final Map<Tab, OrderStatus> tabStatusMap = new HashMap<>();

    private OrderStatus currentStatusFilter = null; // null means All

    public OrderListView(OrderService orderService) {
        this.orderService = orderService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        createHeader();
        createTabs();
        createGrid();
        refreshGrid();
    }

    private void createHeader() {
        H2 title = new H2("Manajemen Antrian Order Laundry");
        title.getStyle().set("margin", "0").set("color", "#7B2D8E");

        Paragraph subtitle = new Paragraph("Pantau status cucian dari proses terima sampai pengambilan oleh pelanggan.");
        subtitle.getStyle().set("margin", "0 0 0.5rem 0").set("color", "#6B6B7B");

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

        HorizontalLayout toolbar = new HorizontalLayout(searchField, newOrderBtn);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

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

        statusTabs.addSelectedChangeListener(e -> {
            currentStatusFilter = tabStatusMap.get(e.getSelectedTab());
            refreshGrid();
        });

        add(statusTabs);
    }

    private void createGrid() {
        grid.setSizeFull();
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

        add(grid);
    }

    private void refreshGrid() {
        String query = searchField.getValue();
        if (currentStatusFilter == null) {
            grid.setItems(orderService.search(query));
        } else {
            grid.setItems(orderService.searchByStatus(currentStatusFilter, query));
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Support ?status=SELESAI navigation from Dashboard
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
