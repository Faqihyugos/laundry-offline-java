package com.example.ui.views.customer;

import com.example.domain.Customer;
import com.example.domain.Order;
import com.example.service.CustomerService;
import com.example.service.OrderService;
import com.example.service.ReceiptService;
import com.example.ui.MainLayout;
import com.example.ui.views.order.OrderDetailView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "customers", layout = MainLayout.class)
@PageTitle("Detail Pelanggan | Laundry POS")
@RolesAllowed({"OWNER", "CASHIER"})
public class CustomerDetailView extends VerticalLayout implements HasUrlParameter<Long> {

    private final CustomerService customerService;
    private final OrderService orderService;
    private final Grid<Order> historyGrid = new Grid<>(Order.class, false);
    private final Div mobileHistoryContainer = new Div();

    private Customer currentCustomer;

    public CustomerDetailView(CustomerService customerService, OrderService orderService) {
        this.customerService = customerService;
        this.orderService = orderService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("max-width", "1280px").set("margin", "0 auto");
    }

    @Override
    public void setParameter(BeforeEvent event, Long customerId) {
        currentCustomer = customerService.findById(customerId).orElse(null);
        removeAll();

        if (currentCustomer == null) {
            add(new H2("Pelanggan tidak ditemukan."));
            Button backBtn = new Button("Kembali ke Daftar Pelanggan", e -> UI.getCurrent().navigate(CustomerListView.class));
            add(backBtn);
            return;
        }

        buildUI();
    }

    private void buildUI() {
        Button backBtn = new Button("Kembali ke Pelanggan", VaadinIcon.ARROW_LEFT.create(),
                e -> UI.getCurrent().navigate(CustomerListView.class));
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        H2 title = new H2(currentCustomer.getName());
        title.getStyle().set("margin", "0").set("color", "#7B2D8E").set("font-weight", "800");

        // Customer Info Card
        Div infoCard = new Div();
        infoCard.addClassName("modern-card");

        Div contacts = new Div();
        contacts.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("align-items", "center").set("gap", "10px");

        Icon phoneIcon = VaadinIcon.PHONE.create();
        phoneIcon.setSize("14px");
        phoneIcon.setColor("#6D28D9");
        Span phoneSpan = new Span(currentCustomer.getPhone());
        phoneSpan.getStyle().set("font-weight", "700");
        HorizontalLayout phoneBox = new HorizontalLayout(phoneIcon, phoneSpan);
        phoneBox.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        phoneBox.getStyle().set("gap", "6px");

        String waUrl = CustomerService.buildWhatsAppUrl(currentCustomer.getPhone(),
                "Halo " + currentCustomer.getName() + ", kami dari Laundry Bersih Wangi.");
        Anchor waLink = new Anchor(waUrl, "Chat WhatsApp");
        waLink.setTarget("_blank");
        waLink.getStyle().set("color", "#059669").set("font-weight", "600");

        Anchor telLink = new Anchor("tel:" + currentCustomer.getPhone(), "Panggil Telepon");
        telLink.getStyle().set("color", "#2563EB").set("font-weight", "600");

        contacts.add(phoneBox, new Span("•"), waLink, new Span("•"), telLink);

        Icon mapIcon = VaadinIcon.MAP_MARKER.create();
        mapIcon.setSize("14px");
        mapIcon.setColor("#64748B");
        Span addrText = new Span(currentCustomer.getAddress() != null && !currentCustomer.getAddress().isEmpty()
                ? currentCustomer.getAddress() : "-");
        addrText.getStyle().set("color", "#64748B");
        HorizontalLayout addrBox = new HorizontalLayout(mapIcon, addrText);
        addrBox.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        addrBox.getStyle().set("gap", "6px").set("margin-top", "0.5rem");

        infoCard.add(contacts, addrBox);

        H3 historyTitle = new H3("Riwayat Transaksi Cucian");
        historyTitle.getStyle().set("margin", "1.5rem 0 0.5rem 0").set("color", "#191426").set("font-weight", "700");

        // History Grid (Desktop)
        historyGrid.setSizeFull();
        historyGrid.addClassName("desktop-only");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        historyGrid.addColumn(Order::getInvoiceNumber).setHeader("No. Invoice").setAutoWidth(true);
        historyGrid.addColumn(o -> o.getOrderDate().format(dtf)).setHeader("Tanggal").setAutoWidth(true);
        historyGrid.addColumn(o -> o.getItems() != null ? o.getItems().size() + " jenis" : "0")
                .setHeader("Item").setAutoWidth(true);
        historyGrid.addColumn(o -> ReceiptService.formatRupiah(o.getTotalAmount()))
                .setHeader("Total (Rp)").setAutoWidth(true);

        historyGrid.addComponentColumn(order -> {
            Span badge = new Span(order.getStatus().getDisplayName());
            badge.addClassName("status-badge");
            badge.addClassName(order.getStatus().getBadgeClass());
            return badge;
        }).setHeader("Status Cucian").setAutoWidth(true);

        historyGrid.addComponentColumn(order -> {
            Span badge = new Span(order.getPaymentStatus().getDisplayName());
            badge.addClassName("status-badge");
            badge.addClassName(order.getPaymentStatus().getBadgeClass());
            return badge;
        }).setHeader("Status Pembayaran").setAutoWidth(true);

        historyGrid.addComponentColumn(order -> {
            Button detailBtn = new Button("Detail", VaadinIcon.ARROW_RIGHT.create(),
                    e -> UI.getCurrent().navigate(OrderDetailView.class, order.getId()));
            detailBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            detailBtn.getStyle().set("color", "#7B2D8E");
            return detailBtn;
        }).setHeader("Aksi").setAutoWidth(true);

        List<Order> orders = orderService.findByCustomer(currentCustomer);
        historyGrid.setItems(orders);
        historyGrid.setAllRowsVisible(true);

        // Mobile History Cards
        mobileHistoryContainer.addClassName("mobile-only");
        mobileHistoryContainer.removeAll();
        if (orders.isEmpty()) {
            Div emptyMsg = new Div();
            emptyMsg.setText("Belum ada riwayat pesanan untuk pelanggan ini.");
            emptyMsg.getStyle().set("color", "#9C96A8").set("padding", "1rem");
            mobileHistoryContainer.add(emptyMsg);
        } else {
            for (Order order : orders) {
                Div card = new Div();
                card.addClassName("mobile-order-card");

                Div r1 = new Div();
                r1.addClassName("mobile-order-card-header");
                Span inv = new Span(order.getInvoiceNumber());
                inv.addClassName("mobile-order-inv");
                Span status = new Span(order.getStatus().getDisplayName());
                status.addClassNames("status-badge", order.getStatus().getBadgeClass());
                r1.add(inv, status);

                Div r2 = new Div();
                r2.getStyle().set("display", "flex").set("justify-content", "space-between").set("align-items", "center");
                Span dt = new Span(order.getOrderDate().format(dtf));
                dt.getStyle().set("color", "#6C667A").set("font-size", "0.85rem");
                Span pay = new Span(order.getPaymentStatus().getDisplayName());
                pay.addClassNames("status-badge", order.getPaymentStatus().getBadgeClass());
                r2.add(dt, pay);

                Div r3 = new Div();
                r3.getStyle().set("display", "flex").set("justify-content", "space-between").set("align-items", "center");
                Span tot = new Span(ReceiptService.formatRupiah(order.getTotalAmount()));
                tot.addClassName("mobile-order-total");

                Button dBtn = new Button("Detail", VaadinIcon.ARROW_RIGHT.create(),
                        e -> UI.getCurrent().navigate(OrderDetailView.class, order.getId()));
                dBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
                dBtn.getStyle().set("background-color", "#7B2D8E");
                r3.add(tot, dBtn);

                card.add(r1, r2, r3);
                mobileHistoryContainer.add(card);
            }
        }

        add(backBtn, title, infoCard, historyTitle, historyGrid, mobileHistoryContainer);
    }
}
