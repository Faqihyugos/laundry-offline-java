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
        getStyle().set("max-width", "1280px")
                .set("margin", "0 auto");

        createHeader();
        createKPIs();
        createWeeklyTrend();
        createRecentOrders();
    }

    private void createHeader() {
        Div headerContainer = new Div();
        headerContainer.setWidthFull();

        Div topRow = new Div();
        topRow.addClassName("responsive-toolbar");

        Div titleGroup = new Div();
        H2 title = new H2("Dashboard Operasional");
        title.getStyle().set("margin", "0")
                .set("color", "#7B2D8E")
                .set("font-size", "1.65rem")
                .set("font-weight", "800");

        Paragraph subtitle = new Paragraph("Ringkasan transaksi hari ini, antrian proses cucian, dan performa mingguan.");
        subtitle.getStyle().set("margin", "4px 0 0 0")
                .set("color", "#6C667A")
                .set("font-size", "0.9rem");
        titleGroup.add(title, subtitle);

        Button newOrderBtn = new Button("Order Baru", VaadinIcon.PLUS.create(),
                e -> UI.getCurrent().navigate(CreateOrderView.class));
        newOrderBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        newOrderBtn.getStyle().set("background-color", "#7B2D8E")
                .set("box-shadow", "0 4px 12px rgba(123, 45, 142, 0.3)");

        topRow.add(titleGroup, newOrderBtn);
        headerContainer.add(topRow);

        add(headerContainer);
    }

    private void createKPIs() {
        DashboardService.DashboardSummary summary = dashboardService.getSummary();

        Div grid = new Div();
        grid.addClassName("kpi-grid");

        // 1. Revenue Card (Gradient Highlight)
        Div revCard = new Div();
        revCard.addClassNames("kpi-card", "kpi-revenue-card");

        Div revHeader = new Div();
        revHeader.addClassName("kpi-card-header");
        Span revTitle = new Span("Pendapatan Hari Ini");
        revTitle.addClassName("kpi-title");
        Span revIcon = new Span("💰");
        revIcon.getStyle().set("font-size", "1.25rem");
        revHeader.add(revTitle, revIcon);

        Div revVal = new Div();
        revVal.addClassName("kpi-value");
        revVal.setText(ReceiptService.formatRupiah(summary.getTodayRevenue()));

        Span revSub = new Span("Total penerimaan kas & transfer");
        revSub.addClassName("kpi-subtitle");

        revCard.add(revHeader, revVal, revSub);

        // 2. Pending Card
        Div pendingCard = createStatusKPICard("Pending", summary.getPendingCount(),
                "⏳", "#1565C0", "#E3F2FD", "Menunggu dicuci", OrderStatus.PENDING);

        // 3. Proses Card
        Div prosesCard = createStatusKPICard("Proses", summary.getProsesCount(),
                "🧺", "#E65100", "#FFF3E0", "Sedang dikerjakan", OrderStatus.PROSES);

        // 4. Selesai Card
        Div selesaiCard = createStatusKPICard("Selesai (Siap)", summary.getSelesaiCount(),
                "✅", "#2E7D32", "#E8F5E9", "Siap diambil", OrderStatus.SELESAI);

        // 5. Diambil Card
        Div diambilCard = createStatusKPICard("Diambil", summary.getDiambilCount(),
                "📦", "#6A1B9A", "#F3E5F5", "Telah diserahkan", OrderStatus.DIAMBIL);

        grid.add(revCard, pendingCard, prosesCard, selesaiCard, diambilCard);
        add(grid);
    }

    private Div createStatusKPICard(String label, long count, String iconEmoji,
                                    String color, String bgTint, String subText, OrderStatus targetStatus) {
        Div card = new Div();
        card.addClassName("kpi-card");
        card.getStyle().set("cursor", "pointer");
        card.addClickListener(e -> UI.getCurrent().navigate("orders?status=" + targetStatus.name()));

        Div header = new Div();
        header.addClassName("kpi-card-header");

        Span title = new Span(label);
        title.addClassName("kpi-title");

        Span iconBadge = new Span(iconEmoji);
        iconBadge.addClassName("kpi-icon-badge");
        iconBadge.getStyle().set("background-color", bgTint);

        header.add(title, iconBadge);

        Div val = new Div();
        val.addClassName("kpi-value");
        val.setText(String.valueOf(count));
        val.getStyle().set("color", color);

        Span sub = new Span(subText);
        sub.getStyle().set("font-size", "0.75rem").set("color", "#9C96A8").set("margin-top", "4px");

        card.add(header, val, sub);
        return card;
    }

    private void createWeeklyTrend() {
        Map<String, BigDecimal> trend = dashboardService.getWeeklyRevenueTrend();

        Div card = new Div();
        card.addClassName("modern-card");

        H3 title = new H3("📊 Grafik Pendapatan Mingguan (7 Hari Terakhir)");
        title.getStyle().set("margin", "0 0 1rem 0")
                .set("font-size", "1.1rem")
                .set("color", "#191426")
                .set("font-weight", "700");
        card.add(title);

        boolean hasAnyRevenue = trend.values().stream().anyMatch(v -> v.compareTo(BigDecimal.ZERO) > 0);

        if (!hasAnyRevenue) {
            Div emptyMsg = new Div();
            emptyMsg.setText("Belum ada data transaksi tercatat dalam 7 hari terakhir.");
            emptyMsg.getStyle().set("color", "#9C96A8")
                    .set("padding", "2.5rem 1rem")
                    .set("text-align", "center")
                    .set("font-size", "0.9rem");
            card.add(emptyMsg);
        } else {
            BigDecimal maxRevenue = trend.values().stream().reduce(BigDecimal.ONE, BigDecimal::max);
            if (maxRevenue.compareTo(BigDecimal.ZERO) == 0) maxRevenue = BigDecimal.ONE;

            Div scrollWrapper = new Div();
            scrollWrapper.getStyle().set("overflow-x", "auto").set("width", "100%");

            HorizontalLayout barsLayout = new HorizontalLayout();
            barsLayout.setWidthFull();
            barsLayout.setMinWidth("480px");
            barsLayout.setHeight("180px");
            barsLayout.setAlignItems(FlexComponent.Alignment.END);
            barsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);

            for (Map.Entry<String, BigDecimal> entry : trend.entrySet()) {
                VerticalLayout barCol = new VerticalLayout();
                barCol.setPadding(false);
                barCol.setSpacing(false);
                barCol.setAlignItems(FlexComponent.Alignment.CENTER);

                Span amountLabel = new Span(ReceiptService.formatRupiah(entry.getValue()));
                amountLabel.getStyle().set("font-size", "0.7rem")
                        .set("color", "#6C667A")
                        .set("font-weight", "600");

                double ratio = entry.getValue().doubleValue() / maxRevenue.doubleValue();
                int barHeight = Math.max((int) (ratio * 120), 6);

                Div bar = new Div();
                bar.setWidth("38px");
                bar.setHeight(barHeight + "px");
                bar.getStyle().set("background", "linear-gradient(180deg, #9B4DB0 0%, #7B2D8E 100%)")
                        .set("border-radius", "8px 8px 0 0")
                        .set("box-shadow", "0 3px 6px rgba(123, 45, 142, 0.25)")
                        .set("transition", "height 0.3s ease");

                Span dateLabel = new Span(entry.getKey());
                dateLabel.getStyle().set("font-size", "0.75rem")
                        .set("font-weight", "600")
                        .set("color", "#191426")
                        .set("margin-top", "6px");

                barCol.add(amountLabel, bar, dateLabel);
                barsLayout.add(barCol);
            }
            scrollWrapper.add(barsLayout);
            card.add(scrollWrapper);
        }

        add(card);
    }

    private void createRecentOrders() {
        List<Order> recentOrders = dashboardService.getLatest5Orders();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        Div card = new Div();
        card.addClassName("modern-card");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        H3 title = new H3("📋 5 Order Terbaru");
        title.getStyle().set("margin", "0")
                .set("font-size", "1.1rem")
                .set("color", "#191426")
                .set("font-weight", "700");

        Button viewAllBtn = new Button("Lihat Semua Order", VaadinIcon.ARROW_RIGHT.create(),
                e -> UI.getCurrent().navigate(OrderListView.class));
        viewAllBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        viewAllBtn.getStyle().set("color", "#7B2D8E").set("font-weight", "600");

        header.add(title, viewAllBtn);
        card.add(header);

        if (recentOrders.isEmpty()) {
            Div emptyState = new Div();
            emptyState.getStyle().set("padding", "3rem 1rem")
                    .set("text-align", "center");

            Span iconSpan = new Span("🧺");
            iconSpan.getStyle().set("font-size", "2.5rem").set("display", "block").set("margin-bottom", "8px");

            Paragraph p = new Paragraph("Belum ada pesanan laundry tercatat.");
            p.getStyle().set("color", "#6C667A").set("font-weight", "600").set("margin", "0 0 1rem 0");

            Button startBtn = new Button("Buat Order Pertama", VaadinIcon.PLUS.create(),
                    e -> UI.getCurrent().navigate(CreateOrderView.class));
            startBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            startBtn.getStyle().set("background-color", "#7B2D8E");

            emptyState.add(iconSpan, p, startBtn);
            card.add(emptyState);
        } else {
            // Desktop Table Grid
            Grid<Order> grid = new Grid<>(Order.class, false);
            grid.addClassName("desktop-only");
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
                detailBtn.getStyle().set("color", "#7B2D8E");
                return detailBtn;
            }).setHeader("Aksi").setAutoWidth(true);

            grid.setItems(recentOrders);
            grid.setAllRowsVisible(true);
            card.add(grid);

            // Mobile Card View (for smartphone screens)
            Div mobileList = new Div();
            mobileList.addClassName("mobile-only");
            mobileList.getStyle().set("margin-top", "12px");

            for (Order order : recentOrders) {
                Div orderCard = new Div();
                orderCard.addClassName("mobile-order-card");

                Div row1 = new Div();
                row1.addClassName("mobile-order-card-header");
                Span inv = new Span(order.getInvoiceNumber());
                inv.addClassName("mobile-order-inv");

                Span statusBadge = new Span(order.getStatus().getDisplayName());
                statusBadge.addClassNames("status-badge", order.getStatus().getBadgeClass());

                row1.add(inv, statusBadge);

                Div row2 = new Div();
                row2.getStyle().set("display", "flex").set("justify-content", "space-between").set("align-items", "center");
                Span cust = new Span("👤 " + (order.getCustomer() != null ? order.getCustomer().getName() : "-"));
                cust.addClassName("mobile-order-customer");

                Span payBadge = new Span(order.getPaymentStatus().getDisplayName());
                payBadge.addClassNames("status-badge", order.getPaymentStatus().getBadgeClass());
                row2.add(cust, payBadge);

                Div row3 = new Div();
                row3.getStyle().set("display", "flex").set("justify-content", "space-between").set("align-items", "center").set("margin-top", "4px");
                Span total = new Span(ReceiptService.formatRupiah(order.getTotalAmount()));
                total.addClassName("mobile-order-total");

                Button detailBtn = new Button("Detail", VaadinIcon.ARROW_RIGHT.create(), e -> {
                    UI.getCurrent().navigate(OrderDetailView.class, order.getId());
                });
                detailBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
                detailBtn.getStyle().set("background-color", "#7B2D8E");

                row3.add(total, detailBtn);

                orderCard.add(row1, row2, row3);
                mobileList.add(orderCard);
            }
            card.add(mobileList);
        }

        add(card);
    }
}
