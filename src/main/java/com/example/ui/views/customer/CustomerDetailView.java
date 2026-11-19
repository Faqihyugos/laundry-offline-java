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

    private Customer currentCustomer;
    private final Div infoCard = new Div();

    public CustomerDetailView(CustomerService customerService, OrderService orderService) {
        this.customerService = customerService;
        this.orderService = orderService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
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
        title.getStyle().set("margin", "0").set("color", "#7B2D8E");

        // Customer Info Card
        infoCard.getStyle().set("background", "white")
                .set("padding", "1.25rem")
                .set("border-radius", "12px")
                .set("box-shadow", "0 1px 4px rgba(0,0,0,0.06)")
                .set("border", "1px solid #F0E6F6")
                .set("width", "100%");

        HorizontalLayout contacts = new HorizontalLayout();
        contacts.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        contacts.setSpacing(true);

        Span phoneSpan = new Span("📱 " + currentCustomer.getPhone());
        phoneSpan.getStyle().set("font-weight", "600");

        String waUrl = CustomerService.buildWhatsAppUrl(currentCustomer.getPhone(),
                "Halo " + currentCustomer.getName() + ", kami dari Laundry Bersih Wangi.");
        Anchor waLink = new Anchor(waUrl, "Chat WhatsApp");
        waLink.setTarget("_blank");
        waLink.getStyle().set("color", "#2E7D32").set("font-weight", "600");

        Anchor telLink = new Anchor("tel:" + currentCustomer.getPhone(), "Panggil Telepon");
        telLink.getStyle().set("color", "#1565C0").set("font-weight", "600");

        contacts.add(phoneSpan, new Span("•"), waLink, new Span("•"), telLink);

        Paragraph addr = new Paragraph("📍 Alamat: " + (currentCustomer.getAddress() != null && !currentCustomer.getAddress().isEmpty()
                ? currentCustomer.getAddress() : "-"));
        addr.getStyle().set("margin", "0.5rem 0 0 0").set("color", "#6B6B7B");

        infoCard.add(contacts, addr);

        H3 historyTitle = new H3("Riwayat Transaksi Cucian");
        historyTitle.getStyle().set("margin", "1.5rem 0 0.5rem 0").set("color", "#1A1A2E");

        // History Grid
        historyGrid.setSizeFull();
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
            return detailBtn;
        }).setHeader("Aksi").setAutoWidth(true);

        List<Order> orders = orderService.findByCustomer(currentCustomer);
        historyGrid.setItems(orders);

        add(backBtn, title, infoCard, historyTitle, historyGrid);
    }
}
