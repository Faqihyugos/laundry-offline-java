package com.example.ui.views.report;

import com.example.domain.Order;
import com.example.domain.PaymentMethod;
import com.example.domain.PaymentStatus;
import com.example.service.ReceiptService;
import com.example.service.ReportService;
import com.example.service.ReportService.ReportPeriod;
import com.example.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.RolesAllowed;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Route(value = "reports", layout = MainLayout.class)
@PageTitle("Laporan Penjualan | Laundry POS")
@RolesAllowed("OWNER")
public class SalesReportView extends VerticalLayout {

    private final ReportService reportService;

    private final Tabs periodTabs = new Tabs();
    private final Map<Tab, ReportPeriod> tabPeriodMap = new HashMap<>();
    private ReportPeriod selectedPeriod = ReportPeriod.BULAN_INI;

    private final DatePicker startDatePicker = new DatePicker("Tanggal Mulai");
    private final DatePicker endDatePicker = new DatePicker("Tanggal Akhir");
    private final HorizontalLayout customDateRow = new HorizontalLayout();

    private final Select<PaymentMethod> methodSelect = new Select<>();
    private final Select<PaymentStatus> statusSelect = new Select<>();

    private final Div kpiRevenueVal = new Div();
    private final Div kpiCountVal = new Div();
    private final Div kpiAvgVal = new Div();

    private final Div trendChartContainer = new Div();
    private final Grid<Order> transactionGrid = new Grid<>(Order.class, false);
    private final Anchor exportAnchor = new Anchor();
    private final Button exportBtn = new Button("Export Excel (.xlsx)", VaadinIcon.FILE_TABLE.create());

    public SalesReportView(ReportService reportService) {
        this.reportService = reportService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("max-width", "1280px").set("margin", "0 auto");

        createHeader();
        createFilters();
        createKPICards();
        createTrendSection();
        createTransactionGrid();
        refreshReport();
    }

    private void createHeader() {
        H2 title = new H2("Laporan Penjualan & Keuangan");
        title.getStyle().set("margin", "0")
                .set("color", "var(--text-main)")
                .set("font-size", "1.6rem")
                .set("font-weight", "800")
                .set("letter-spacing", "-0.02em");

        Paragraph subtitle = new Paragraph("Analisis performa pendapatan, volume transaksi harian/mingguan/bulanan, dan unduh laporan Excel.");
        subtitle.getStyle().set("margin", "4px 0 0.5rem 0").set("color", "var(--text-muted)").set("font-size", "0.88rem");

        exportBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        exportAnchor.add(exportBtn);

        Div toolbar = new Div();
        toolbar.addClassName("responsive-toolbar");
        toolbar.add(title, exportAnchor);

        add(toolbar, subtitle);
    }

    private void createFilters() {
        Tab tabHari = new Tab("Hari Ini");
        tabPeriodMap.put(tabHari, ReportPeriod.HARI_INI);

        Tab tabMinggu = new Tab("Minggu Ini");
        tabPeriodMap.put(tabMinggu, ReportPeriod.MINGGU_INI);

        Tab tabBulan = new Tab("Bulan Ini");
        tabPeriodMap.put(tabBulan, ReportPeriod.BULAN_INI);

        Tab tabCustom = new Tab("Custom Range");
        tabPeriodMap.put(tabCustom, ReportPeriod.CUSTOM);

        periodTabs.add(tabHari, tabMinggu, tabBulan, tabCustom);
        periodTabs.setSelectedTab(tabBulan);
        periodTabs.addClassName("scrollable-tabs");

        periodTabs.addSelectedChangeListener(e -> {
            selectedPeriod = tabPeriodMap.get(e.getSelectedTab());
            customDateRow.setVisible(selectedPeriod == ReportPeriod.CUSTOM);
            refreshReport();
        });

        startDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        endDatePicker.setValue(LocalDate.now());

        startDatePicker.addValueChangeListener(e -> refreshReport());
        endDatePicker.addValueChangeListener(e -> refreshReport());

        customDateRow.add(startDatePicker, endDatePicker);
        customDateRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        customDateRow.setVisible(false);

        methodSelect.setLabel("Metode Pembayaran");
        methodSelect.setItems(PaymentMethod.values());
        methodSelect.setItemLabelGenerator(m -> m == null ? "Semua Metode" : m.getDisplayName());
        methodSelect.setEmptySelectionAllowed(true);
        methodSelect.setEmptySelectionCaption("Semua Metode");
        methodSelect.addValueChangeListener(e -> refreshReport());

        statusSelect.setLabel("Status Bayar");
        statusSelect.setItems(PaymentStatus.values());
        statusSelect.setItemLabelGenerator(s -> s == null ? "Semua Status" : s.getDisplayName());
        statusSelect.setEmptySelectionAllowed(true);
        statusSelect.setEmptySelectionCaption("Semua Status");
        statusSelect.addValueChangeListener(e -> refreshReport());

        Div filterContainer = new Div();
        filterContainer.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "12px").set("align-items", "center");
        filterContainer.add(periodTabs, customDateRow, methodSelect, statusSelect);

        add(filterContainer);
    }

    private void createKPICards() {
        Div kpiRow = new Div();
        kpiRow.getStyle().set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(220px, 1fr))")
                .set("gap", "14px")
                .set("width", "100%");

        Div cardRev = createKPICard("Total Pendapatan", kpiRevenueVal, VaadinIcon.MONEY_DEPOSIT.create(), "#6D28D9", "#EDE9FE");
        Div cardCount = createKPICard("Jumlah Transaksi", kpiCountVal, VaadinIcon.INVOICE.create(), "#2563EB", "#EFF6FF");
        Div cardAvg = createKPICard("Rata-Rata Order", kpiAvgVal, VaadinIcon.CHART_LINE.create(), "#059669", "#ECFDF5");

        kpiRow.add(cardRev, cardCount, cardAvg);
        add(kpiRow);
    }

    private Div createKPICard(String label, Div valContainer, Icon icon, String color, String bgTint) {
        Div card = new Div();
        card.addClassName("kpi-card");

        Div header = new Div();
        header.addClassName("kpi-card-header");

        Span title = new Span(label);
        title.addClassName("kpi-title");

        Div iconBadge = new Div();
        iconBadge.addClassName("kpi-icon-box");
        icon.setSize("20px");
        icon.setColor(color);
        iconBadge.add(icon);
        iconBadge.getStyle().set("background-color", bgTint);

        header.add(title, iconBadge);

        valContainer.addClassName("kpi-value");
        valContainer.setText("-");
        valContainer.getStyle().set("color", color);

        card.add(header, valContainer);
        return card;
    }

    private void createTrendSection() {
        trendChartContainer.addClassName("modern-card");
        add(trendChartContainer);
    }

    private void createTransactionGrid() {
        transactionGrid.setSizeFull();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        transactionGrid.addColumn(Order::getInvoiceNumber).setHeader("No. Invoice").setAutoWidth(true);
        transactionGrid.addColumn(o -> o.getOrderDate().format(dtf)).setHeader("Tanggal").setAutoWidth(true);
        transactionGrid.addColumn(o -> o.getCustomer() != null ? o.getCustomer().getName() : "-").setHeader("Pelanggan").setAutoWidth(true);

        transactionGrid.addComponentColumn(order -> {
            Span badge = new Span(order.getStatus().getDisplayName());
            badge.addClassName("status-badge");
            badge.addClassName(order.getStatus().getBadgeClass());
            return badge;
        }).setHeader("Status Cucian").setAutoWidth(true);

        transactionGrid.addComponentColumn(order -> {
            Span badge = new Span(order.getPaymentStatus().getDisplayName());
            badge.addClassName("status-badge");
            badge.addClassName(order.getPaymentStatus().getBadgeClass());
            return badge;
        }).setHeader("Status Bayar").setAutoWidth(true);

        transactionGrid.addColumn(o -> o.getPaymentMethod().name()).setHeader("Metode").setAutoWidth(true);
        transactionGrid.addColumn(o -> ReceiptService.formatRupiah(o.getTotalAmount())).setHeader("Total").setAutoWidth(true);
        transactionGrid.addColumn(o -> ReceiptService.formatRupiah(o.getPaidAmount())).setHeader("Terbayar").setAutoWidth(true);
        transactionGrid.addColumn(o -> ReceiptService.formatRupiah(o.getRemainingAmount())).setHeader("Sisa").setAutoWidth(true);

        add(new H3("Daftar Transaksi Periode Ini"), transactionGrid);
    }

    private void refreshReport() {
        try {
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();
            PaymentMethod method = methodSelect.getValue();
            PaymentStatus status = statusSelect.getValue();

            List<Order> orders = reportService.getOrdersForReport(selectedPeriod, start, end, method, status);
            transactionGrid.setItems(orders);

            BigDecimal total = reportService.calculateTotalRevenue(orders);
            BigDecimal avg = reportService.calculateAverageOrderValue(orders);

            kpiRevenueVal.setText(ReceiptService.formatRupiah(total));
            kpiCountVal.setText(orders.size() + " Pesanan");
            kpiAvgVal.setText(ReceiptService.formatRupiah(avg));

            renderTrendChart(orders);
            updateExcelExportAnchor(orders);
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void renderTrendChart(List<Order> orders) {
        trendChartContainer.removeAll();

        H3 title = new H3("Tren Pendapatan Penjualan");
        title.getStyle().set("margin", "0 0 1rem 0").set("font-size", "1.1rem").set("color", "#191426").set("font-weight", "700");
        trendChartContainer.add(title);

        if (orders.isEmpty()) {
            Div empty = new Div();
            empty.setText("Belum ada data transaksi pada periode yang dipilih.");
            empty.getStyle().set("color", "#9C96A8").set("padding", "2rem").set("text-align", "center");
            trendChartContainer.add(empty);
            return;
        }

        LocalDateTime[] range = reportService.calculateDateRange(selectedPeriod, startDatePicker.getValue(), endDatePicker.getValue());
        Map<String, BigDecimal> trend = reportService.calculateDailyTrend(orders, range[0], range[1]);

        BigDecimal max = trend.values().stream().reduce(BigDecimal.ONE, BigDecimal::max);
        if (max.compareTo(BigDecimal.ZERO) == 0) max = BigDecimal.ONE;

        Div scrollWrap = new Div();
        scrollWrap.getStyle().set("overflow-x", "auto").set("width", "100%");

        HorizontalLayout bars = new HorizontalLayout();
        bars.setWidthFull();
        bars.setMinWidth("480px");
        bars.setHeight("180px");
        bars.setAlignItems(FlexComponent.Alignment.END);
        bars.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);

        for (Map.Entry<String, BigDecimal> entry : trend.entrySet()) {
            VerticalLayout col = new VerticalLayout();
            col.setPadding(false);
            col.setSpacing(false);
            col.setAlignItems(FlexComponent.Alignment.CENTER);

            Span amountLabel = new Span(ReceiptService.formatRupiah(entry.getValue()));
            amountLabel.getStyle().set("font-size", "0.65rem").set("color", "#6C667A").set("font-weight", "600");

            double ratio = entry.getValue().doubleValue() / max.doubleValue();
            int h = Math.max((int) (ratio * 120), 4);

            Div bar = new Div();
            bar.setWidth("28px");
            bar.setHeight(h + "px");
            bar.getStyle().set("background", "linear-gradient(180deg, #6366F1 0%, #4F46E5 100%)")
                    .set("border-radius", "6px 6px 0 0")
                    .set("box-shadow", "0 2px 4px rgba(79, 70, 229, 0.2)");

            Span dateLabel = new Span(entry.getKey());
            dateLabel.getStyle().set("font-size", "0.7rem").set("margin-top", "4px").set("font-weight", "600");

            col.add(amountLabel, bar, dateLabel);
            bars.add(col);
        }
        scrollWrap.add(bars);
        trendChartContainer.add(scrollWrap);
    }

    private void updateExcelExportAnchor(List<Order> orders) {
        String filename = "laporan_penjualan_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";

        StreamResource resource = new StreamResource(filename, () -> {
            try {
                byte[] bytes = reportService.generateExcelReport(orders, selectedPeriod, startDatePicker.getValue(), endDatePicker.getValue());
                return new ByteArrayInputStream(bytes);
            } catch (Exception ex) {
                return new ByteArrayInputStream(new byte[0]);
            }
        });
        resource.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        exportAnchor.setHref(resource);
        exportAnchor.getElement().setAttribute("download", filename);
    }
}
