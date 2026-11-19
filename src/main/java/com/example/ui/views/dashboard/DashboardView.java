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
import com.vaadin.flow.component.icon.Icon;
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
@PageTitle("Overview | Laundry POS")
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
        getStyle().set("max-width", "1360px").set("margin", "0 auto");

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
        H2 title = new H2("Overview Operasional");
        title.getStyle().set("margin", "0")
                .set("color", "#0F172A")
                .set("font-size", "1.6rem")
                .set("font-weight", "800")
                .set("letter-spacing", "-0.02em");

        Paragraph subtitle = new Paragraph("Ringkasan antrian cucian, performa transaksi kasir, dan analitik toko.");
        subtitle.getStyle().set("margin", "4px 0 0 0")
                .set("color", "#64748B")
                .set("font-size", "0.88rem");
        titleGroup.add(title, subtitle);

        Button newOrderBtn = new Button("Buka Kasir POS", VaadinIcon.CART.create(),
                e -> UI.getCurrent().navigate(CreateOrderView.class));
        newOrderBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newOrderBtn.getStyle().set("background", "var(--primary-gradient)")
                .set("font-weight", "700")
                .set("box-shadow", "0 4px 14px rgba(79, 70, 229, 0.35)")
                .set("border-radius", "12px");

        topRow.add(titleGroup, newOrderBtn);
        headerContainer.add(topRow);

        add(headerContainer);
    }

    private void createKPIs() {
        DashboardService.DashboardSummary summary = dashboardService.getSummary();

        Div grid = new Div();
        grid.addClassName("kpi-grid");

        // Uniform, high-tactile cards for all 5 metrics
        Div revCard = createKPIItem("Total Pendapatan", ReceiptService.formatRupiah(summary.getTodayRevenue()),
                VaadinIcon.MONEY_DEPOSIT.create(), "#4F46E5", "linear-gradient(135deg, #EEF2FF 0%, #E0E7FF 100%)",
                "+12.5% vs kemarin", "#ECFDF5", "#059669", null, "#4F46E5");

        Div pendingCard = createKPIItem("Antrian Pending", String.valueOf(summary.getPendingCount()),
                VaadinIcon.CLOCK.create(), "#2563EB", "linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%)",
                "Menunggu Cuci", "#EFF6FF", "#2563EB", OrderStatus.PENDING, "#2563EB");

        Div prosesCard = createKPIItem("Sedang Dicuci", String.valueOf(summary.getProsesCount()),
                VaadinIcon.COGS.create(), "#D97706", "linear-gradient(135deg, #FFFBEB 0%, #FEF3C7 100%)",
                "Dalam Mesin", "#FFFBEB", "#D97706", OrderStatus.PROSES, "#D97706");

        Div selesaiCard = createKPIItem("Siap Diambil", String.valueOf(summary.getSelesaiCount()),
                VaadinIcon.CHECK_CIRCLE.create(), "#059669", "linear-gradient(135deg, #ECFDF5 0%, #D1FAE5 100%)",
                "Siap Kasir", "#ECFDF5", "#059669", OrderStatus.SELESAI, "#059669");

        Div diambilCard = createKPIItem("Sudah Diambil", String.valueOf(summary.getDiambilCount()),
                VaadinIcon.ARCHIVE.create(), "#7C3AED", "linear-gradient(135deg, #F5F3FF 0%, #EDE9FE 100%)",
                "Riwayat Sukses", "#F5F3FF", "#7C3AED", OrderStatus.DIAMBIL, "#7C3AED");

        grid.add(revCard, pendingCard, prosesCard, selesaiCard, diambilCard);
        add(grid);
    }

    private Div createKPIItem(String label, String value, Icon icon,
                              String iconColor, String iconBg,
                              String trendText, String trendBg, String trendColor,
                              OrderStatus targetStatus, String accentColor) {
        Div card = new Div();
        card.addClassName("kpi-card");
        if (targetStatus != null) {
            card.getStyle().set("cursor", "pointer");
            card.addClickListener(e -> UI.getCurrent().navigate("orders?status=" + targetStatus.name()));
        }

        Div header = new Div();
        header.addClassName("kpi-card-header");

        Div iconBox = new Div();
        iconBox.addClassName("kpi-icon-box");
        icon.setSize("22px");
        icon.setColor(iconColor);
        iconBox.add(icon);
        iconBox.getStyle().set("background", iconBg);

        Span trendPill = new Span(trendText);
        trendPill.addClassName("kpi-trend-pill");
        trendPill.getStyle().set("background-color", trendBg).set("color", trendColor);

        header.add(iconBox, trendPill);

        Div body = new Div();
        Span title = new Span(label);
        title.addClassName("kpi-title");

        Div val = new Div();
        val.addClassName("kpi-value");
        val.setText(value);

        body.add(title, val);

        Div accentBar = new Div();
        accentBar.addClassName("kpi-accent-bar");
        accentBar.getStyle().set("background-color", accentColor);

        card.add(header, body, accentBar);
        return card;
    }

    private void createWeeklyTrend() {
        Map<String, BigDecimal> trend = dashboardService.getWeeklyRevenueTrend();

        Div card = new Div();
        card.addClassName("modern-card");

        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        titleRow.getStyle().set("gap", "8px").set("margin-bottom", "1rem");
        Icon chartIcon = VaadinIcon.CHART_LINE.create();
        chartIcon.setSize("20px");
        chartIcon.setColor("#4F46E5");
        H3 title = new H3("Grafik Pendapatan Mingguan (7 Hari Terakhir)");
        title.getStyle().set("margin", "0")
                .set("font-size", "1.1rem")
                .set("color", "#0F172A")
                .set("font-weight", "800");
        titleRow.add(chartIcon, title);
        card.add(titleRow);

        boolean hasAnyRevenue = trend.values().stream().anyMatch(v -> v.compareTo(BigDecimal.ZERO) > 0);

        if (!hasAnyRevenue) {
            Div emptyMsg = new Div();
            emptyMsg.setText("Belum ada data transaksi tercatat dalam 7 hari terakhir.");
            emptyMsg.getStyle().set("color", "#94A3B8")
                    .set("padding", "2.5rem 1rem")
                    .set("text-align", "center")
                    .set("font-size", "0.88rem");
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
                amountLabel.getStyle().set("font-size", "0.72rem")
                        .set("color", "#64748B")
                        .set("font-weight", "700");

                double ratio = entry.getValue().doubleValue() / maxRevenue.doubleValue();
                int barHeight = Math.max((int) (ratio * 120), 8);

                Div bar = new Div();
                bar.setWidth("42px");
                bar.setHeight(barHeight + "px");
                bar.getStyle().set("background", "linear-gradient(180deg, #6366F1 0%, #4F46E5 100%)")
                        .set("border-radius", "8px 8px 0 0")
                        .set("box-shadow", "0 2px 8px rgba(79, 70, 229, 0.25)")
                        .set("transition", "all 0.2s ease");

                Span dateLabel = new Span(entry.getKey());
                dateLabel.getStyle().set("font-size", "0.75rem")
                        .set("font-weight", "700")
                        .set("color", "#334155")
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

        H3 title = new H3("5 Order Terbaru");
        title.getStyle().set("margin", "0")
                .set("font-size", "1.05rem")
                .set("color", "#0F172A")
                .set("font-weight", "700");

        Button viewAllBtn = new Button("Lihat Semua", VaadinIcon.ARROW_RIGHT.create(),
                e -> UI.getCurrent().navigate(OrderListView.class));
        viewAllBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        viewAllBtn.getStyle().set("color", "#6D28D9").set("font-weight", "600");

        header.add(title, viewAllBtn);
        card.add(header);

        if (recentOrders.isEmpty()) {
            Div emptyState = new Div();
            emptyState.getStyle().set("padding", "3rem 1rem").set("text-align", "center");

            Icon emptyIcon = VaadinIcon.INBOX.create();
            emptyIcon.setSize("36px");
            emptyIcon.setColor("#94A3B8");

            Paragraph p = new Paragraph("Belum ada pesanan laundry tercatat.");
            p.getStyle().set("color", "#64748B").set("font-weight", "600").set("margin", "8px 0 1rem 0");

            Button startBtn = new Button("Buka Kasir POS", VaadinIcon.CART.create(),
                    e -> UI.getCurrent().navigate(CreateOrderView.class));
            startBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            startBtn.getStyle().set("background-color", "#6D28D9");

            emptyState.add(emptyIcon, p, startBtn);
            card.add(emptyState);
        } else {
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
                detailBtn.getStyle().set("color", "#6D28D9").set("font-weight", "600");
                return detailBtn;
            }).setHeader("Aksi").setAutoWidth(true);

            grid.setItems(recentOrders);
            grid.setAllRowsVisible(true);
            card.add(grid);

            // Mobile Card View
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
                Span cust = new Span(order.getCustomer() != null ? order.getCustomer().getName() : "-");
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
                detailBtn.getStyle().set("background-color", "#6D28D9");

                row3.add(total, detailBtn);

                orderCard.add(row1, row2, row3);
                mobileList.add(orderCard);
            }
            card.add(mobileList);
        }

        add(card);
    }
}
