package com.example.ui.views.dashboard;

import com.example.domain.Order;
import com.example.domain.OrderStatus;
import com.example.security.AuthenticatedUser;
import com.example.service.DashboardService;
import com.example.service.ReceiptService;
import com.example.ui.MainLayout;
import com.example.ui.views.order.CreateOrderView;
import com.example.ui.views.order.OrderDetailView;
import com.example.ui.views.order.OrderListView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "dashboard", layout = MainLayout.class)
@PageTitle("Dashboard | Laundry POS")
@RolesAllowed({"OWNER", "CASHIER"})
public class DashboardView extends VerticalLayout {

    private final DashboardService dashboardService;
    private final AuthenticatedUser authenticatedUser;

    public DashboardView(DashboardService dashboardService, AuthenticatedUser authenticatedUser) {
        this.dashboardService = dashboardService;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        createHeader();
        createKPIs();
        createWeeklyTrend();
        createRecentOrders();
    }

    private void createHeader() {
        H2 title = new H2("Dashboard Operasional");
        title.getStyle().set("margin", "0").set("color", "#7B2D8E");

        Paragraph subtitle = new Paragraph("Ringkasan transaksi hari ini, antrian proses cucian, dan performa mingguan.");
        subtitle.getStyle().set("margin", "0 0 1rem 0").set("color", "#6B6B7B");

        Button newOrderBtn = new Button("Order Baru", VaadinIcon.PLUS.create(),
                e -> UI.getCurrent().navigate(CreateOrderView.class));
        newOrderBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newOrderBtn.getStyle().set("background-color", "#7B2D8E");

        HorizontalLayout toolbar = new HorizontalLayout(title, newOrderBtn);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        add(toolbar, subtitle);
    }

    private void createKPIs() {
        DashboardService.DashboardSummary summary = dashboardService.getSummary();

        HorizontalLayout kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();
        kpiRow.setSpacing(true);

        // Revenue Card (Owner view, or cash register for cashier)
        Div revCard = new Div();
        revCard.addClassName("kpi-card");
        revCard.setWidth("25%");
        Div revTitle = new Div();
        revTitle.addClassName("kpi-title");
        revTitle.setText("Pendapatan Hari Ini");
        Div revVal = new Div();
        revVal.addClassName("kpi-value");
        revVal.setText(ReceiptService.formatRupiah(summary.getTodayRevenue()));
        revVal.getStyle().set("color", "#7B2D8E");
        revCard.add(revTitle, revVal);

        // Status Distribution Cards (Clickable to pre-filtered order list)
        Div pendingCard = createStatusKPICard("Pending", summary.getPendingCount(), "badge-pending", "#1565C0", OrderStatus.PENDING);
        Div prosesCard = createStatusKPICard("Proses", summary.getProsesCount(), "badge-proses", "#E65100", OrderStatus.PROSES);
        Div selesaiCard = createStatusKPICard("Selesai (Siap)", summary.getSelesaiCount(), "badge-selesai", "#2E7D32", OrderStatus.SELESAI);
        Div diambilCard = createStatusKPICard("Diambil", summary.getDiambilCount(), "badge-diambil", "#6A1B9A", OrderStatus.DIAMBIL);

        kpiRow.add(revCard, pendingCard, prosesCard, selesaiCard, diambilCard);
        add(kpiRow);
    }

    private Div createStatusKPICard(String label, long count, String badgeClass, String color, OrderStatus targetStatus) {
        Div card = new Div();
        card.addClassName("kpi-card");
        card.setWidth("18.75%");
        card.getStyle().set("cursor", "pointer");
        card.addClickListener(e -> {
            UI.getCurrent().navigate("orders?status=" + targetStatus.name());
        });

        Div title = new Div();
        title.addClassName("kpi-title");
        title.setText(label);

        Div val = new Div();
        val.addClassName("kpi-value");
        val.setText(String.valueOf(count));
        val.getStyle().set("color", color);

        card.add(title, val);
        return card;
    }

    private void createWeeklyTrend() {
        Map<String, BigDecimal> trend = dashboardService.getWeeklyRevenueTrend();

        Div card = new Div();
        card.getStyle().set("background", "white")
                .set("padding", "1.25rem")
                .set("border-radius", "12px")
                .set("box-shadow", "0 1px 4px rgba(0,0,0,0.06)")
                .set("border", "1px solid #F0E6F6")
                .set("width", "100%");

        H3 title = new H3("📊 Grafik Pendapatan Mingguan (7 Hari Terakhir)");
        title.getStyle().set("margin", "0 0 1rem 0").set("font-size", "1.1rem").set("color", "#1A1A2E");
        card.add(title);

        HorizontalLayout barsLayout = new HorizontalLayout();
        barsLayout.setWidthFull();
        barsLayout.setHeight("180px");
        barsLayout.setAlignItems(FlexComponent.Alignment.END);
        barsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);

        BigDecimal maxRevenue = trend.values().stream().reduce(BigDecimal.ONE, BigDecimal::max);
        if (maxRevenue.compareTo(BigDecimal.ZERO) == 0) maxRevenue = BigDecimal.ONE;

        boolean hasAnyRevenue = trend.values().stream().anyMatch(v -> v.compareTo(BigDecimal.ZERO) > 0);

        if (!hasAnyRevenue) {
            Div emptyMsg = new Div();
            emptyMsg.setText("Belum ada data transaksi tercatat dalam 7 hari terakhir.");
            emptyMsg.getStyle().set("color", "#9E9E9E").set("padding", "2rem").set("text-align", "center");
            card.add(emptyMsg);
        } else {
            for (Map.Entry<String, BigDecimal> entry : trend.entrySet()) {
                VerticalLayout barCol = new VerticalLayout();
                barCol.setPadding(false);
                barCol.setSpacing(false);
                barCol.setAlignItems(FlexComponent.Alignment.CENTER);

                Span amountLabel = new Span(ReceiptService.formatRupiah(entry.getValue()));
                amountLabel.getStyle().set("font-size", "0.7rem").set("color", "#6B6B7B");

                double ratio = entry.getValue().doubleValue() / maxRevenue.doubleValue();
                int barHeight = Math.max((int) (ratio * 120), 4);

                Div bar = new Div();
                bar.setWidth("36px");
                bar.setHeight(barHeight + "px");
                bar.getStyle().set("background", "linear-gradient(180deg, #9B4DB0 0%, #7B2D8E 100%)")
                        .set("border-radius", "6px 6px 0 0")
                        .set("box-shadow", "0 2px 4px rgba(123, 45, 142, 0.2)");

                Span dateLabel = new Span(entry.getKey());
                dateLabel.getStyle().set("font-size", "0.75rem").set("font-weight", "600").set("margin-top", "4px");

                barCol.add(amountLabel, bar, dateLabel);
                barsLayout.add(barCol);
            }
            card.add(barsLayout);
        }

        add(card);
    }

    private void createRecentOrders() {
        List<Order> recentOrders = dashboardService.getLatest5Orders();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        Div card = new Div();
        card.getStyle().set("background", "white")
                .set("padding", "1.25rem")
                .set("border-radius", "12px")
                .set("box-shadow", "0 1px 4px rgba(0,0,0,0.06)")
                .set("border", "1px solid #F0E6F6")
                .set("width", "100%");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        H3 title = new H3("📋 5 Order Terbaru");
        title.getStyle().set("margin", "0").set("font-size", "1.1rem").set("color", "#1A1A2E");

        Button viewAllBtn = new Button("Lihat Semua Order", VaadinIcon.ARROW_RIGHT.create(),
                e -> UI.getCurrent().navigate(OrderListView.class));
        viewAllBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        header.add(title, viewAllBtn);
        card.add(header);

        if (recentOrders.isEmpty()) {
            Div emptyState = new Div();
            emptyState.getStyle().set("padding", "2rem").set("text-align", "center");
            Paragraph p = new Paragraph("Belum ada pesanan laundry. Buat order pertama Anda sekarang!");
            p.getStyle().set("color", "#6B6B7B");
            Button startBtn = new Button("Buat Order Pertama", VaadinIcon.PLUS.create(),
                    e -> UI.getCurrent().navigate(CreateOrderView.class));
            startBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            startBtn.getStyle().set("background-color", "#7B2D8E");
            emptyState.add(p, startBtn);
            card.add(emptyState);
        } else {
            Grid<Order> grid = new Grid<>(Order.class, false);
            grid.addColumn(Order::getInvoiceNumber).setHeader("No. Invoice").setAutoWidth(true);
            grid.addColumn(o -> o.getCustomer() != null ? o.getCustomer().getName() : "-").setHeader("Pelanggan").setAutoWidth(true);
            grid.addColumn(o -> o.getOrderDate().format(dtf)).setHeader("Tanggal").setAutoWidth(true);
            grid.addColumn(o -> ReceiptService.formatRupiah(o.getTotalAmount())).setHeader("Total").setAutoWidth(true);

            grid.addComponentColumn(order -> {
                Span badge = new Span(order.getPaymentStatus().getDisplayName());
                badge.addClassName("status-badge");
                badge.addClassName(order.getPaymentStatus().getBadgeClass());
                return badge;
            }).setHeader("Status Bayar").setAutoWidth(true);

            grid.addComponentColumn(order -> {
                Span badge = new Span(order.getStatus().getDisplayName());
                badge.addClassName("status-badge");
                badge.addClassName(order.getStatus().getBadgeClass());
                return badge;
            }).setHeader("Status Cucian").setAutoWidth(true);

            grid.addComponentColumn(order -> {
                Button detailBtn = new Button("Detail", e -> {
                    UI.getCurrent().navigate(OrderDetailView.class, order.getId());
                });
                detailBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                return detailBtn;
            }).setHeader("Aksi").setAutoWidth(true);

            grid.setItems(recentOrders);
            grid.setAllRowsVisible(true);
            card.add(grid);
        }

        add(card);
    }
}
