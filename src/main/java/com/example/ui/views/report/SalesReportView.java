package com.example.ui.views.report;

import com.example.domain.Order;
import com.example.domain.PaymentMethod;
import com.example.domain.PaymentStatus;
import com.example.service.ReceiptService;
import com.example.service.ReportService;
import com.example.service.ReportService.ReportPeriod;
import com.example.service.SettingService;
import com.example.ui.MainLayout;
import com.example.ui.components.ThermalReceiptDialog;
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
import java.util.Locale;
import java.util.Map;

@Route(value = "reports", layout = MainLayout.class)
@PageTitle("Laporan Penjualan | Laundry POS")
@RolesAllowed("OWNER")
public class SalesReportView extends VerticalLayout {

    private final ReportService reportService;
    private final SettingService settingService;
    private final ReceiptService receiptService;

    private final Tabs periodTabs = new Tabs();
    private final Map<Tab, ReportPeriod> tabPeriodMap = new HashMap<>();
    private ReportPeriod selectedPeriod = ReportPeriod.BULAN_INI;

    private final DatePicker startDatePicker = new DatePicker("From");
    private final DatePicker endDatePicker = new DatePicker("To");

    private final Select<PaymentMethod> methodSelect = new Select<>();
    private final Select<PaymentStatus> statusSelect = new Select<>();

    // Selected Report Category matching Figma node 15:5991
    private String selectedCategory = "Summary Sales Report";
    private final Div categoriesGrid = new Div();

    // Summary Elements in Right Panel matching Figma node 15:5991
    private final Span dateRangeLabel = new Span();
    private final Span subtotalSpan = new Span("Rp 0");
    private final Span discountSpan = new Span("Rp 0");
    private final Span taxSpan = new Span("Rp 0");
    private final Span totalRevenueSpan = new Span("Rp 0");
    private final Span cashRevenueSpan = new Span("Rp 0");
    private final Span qrisRevenueSpan = new Span("Rp 0");
    private final Span totalPaymentSpan = new Span("Rp 0");

    private final Div trendChartContainer = new Div();
    private final Grid<Order> transactionGrid = new Grid<>(Order.class, false);
    private final Anchor exportAnchor = new Anchor();
    private final Button exportBtn = new Button("Export Excel (.xlsx)", VaadinIcon.FILE_TABLE.create());

    private List<Order> currentOrders = List.of();

    public SalesReportView(ReportService reportService, SettingService settingService, ReceiptService receiptService) {
        this.reportService = reportService;
        this.settingService = settingService;
        this.receiptService = receiptService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("padding", "24px 32px").set("background-color", "#FAFAFA");

        createHeader();
        createFilters();
        createFigmaContentSection();
        createTransactionGrid();
        refreshReport();
    }

    private void createHeader() {
        H2 title = new H2("Report");
        title.getStyle().set("margin", "0")
                .set("color", "#3949ab")
                .set("font-size", "28px")
                .set("font-weight", "600")
                .set("font-family", "'Barlow', sans-serif");

        LocalDate today = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEEE d MMMM, yyyy", new Locale("id", "ID"));
        Paragraph subtitle = new Paragraph(today.format(dtf));
        subtitle.getStyle().set("margin", "4px 0 16px 0").set("color", "#abbbc2").set("font-size", "16px").set("font-family", "'Barlow', sans-serif");

        Div headerContainer = new Div();
        headerContainer.setWidthFull();
        headerContainer.getStyle().set("border-bottom", "1px solid #c8d1e1").set("padding-bottom", "12px").set("margin-bottom", "16px");

        HorizontalLayout titleRow = new HorizontalLayout(new Div(title, subtitle));
        titleRow.setWidthFull();
        titleRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        titleRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        exportBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        exportBtn.getStyle().set("background", "#3949ab").set("border-radius", "8px").set("font-family", "'Barlow', sans-serif");
        exportAnchor.add(exportBtn);
        titleRow.add(exportAnchor);

        headerContainer.add(titleRow);
        add(headerContainer);
    }

    private void createFilters() {
        Tab tabHari = new Tab("Hari Ini");
        tabPeriodMap.put(tabHari, ReportPeriod.HARI_INI);

        Tab tabMinggu = new Tab("Minggu Ini");
        tabPeriodMap.put(tabMinggu, ReportPeriod.MINGGU_INI);

        Tab tabBulan = new Tab("Bulan Ini");
        tabPeriodMap.put(tabBulan, ReportPeriod.BULAN_INI);

        Tab tabCustom = new Tab("Rentang Tanggal");
        tabPeriodMap.put(tabCustom, ReportPeriod.CUSTOM);

        periodTabs.add(tabHari, tabMinggu, tabBulan, tabCustom);
        periodTabs.setSelectedTab(tabBulan);
        periodTabs.addClassName("scrollable-tabs");

        startDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        endDatePicker.setValue(LocalDate.now());

        startDatePicker.addValueChangeListener(e -> refreshReport());
        endDatePicker.addValueChangeListener(e -> refreshReport());

        HorizontalLayout dateRow = new HorizontalLayout(startDatePicker, endDatePicker);
        dateRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);

        periodTabs.addSelectedChangeListener(e -> {
            selectedPeriod = tabPeriodMap.get(e.getSelectedTab());
            dateRow.setVisible(selectedPeriod == ReportPeriod.CUSTOM);
            refreshReport();
        });
        dateRow.setVisible(false);

        methodSelect.setLabel("Metode Bayar");
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
        filterContainer.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "16px").set("align-items", "flex-end").set("margin-bottom", "20px");
        filterContainer.add(periodTabs, dateRow, methodSelect, statusSelect);

        add(filterContainer);
    }

    private void createFigmaContentSection() {
        Div mainRow = new Div();
        mainRow.getStyle()
                .set("display", "flex")
                .set("gap", "24px")
                .set("align-items", "flex-start")
                .set("width", "100%")
                .set("max-width", "100%");

        // 1. Left: 6 Category Cards & Trend Chart
        Div leftCol = new Div();
        leftCol.getStyle()
                .set("flex", "1 1 auto")
                .set("min-width", "0")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "20px");

        categoriesGrid.addClassName("report-categories-grid");
        renderCategoryCards();

        trendChartContainer.addClassName("modern-card");
        trendChartContainer.setWidthFull();

        leftCol.add(categoriesGrid, trendChartContainer);

        // 2. Right: Summary Sales Report Panel (360px, Figma node 15:5991)
        Div rightSummaryCard = createRightSummaryCard();
        rightSummaryCard.getStyle().set("flex", "0 0 360px").set("width", "360px");

        mainRow.add(leftCol, rightSummaryCard);
        add(mainRow);
    }

    private void renderCategoryCards() {
        categoriesGrid.removeAll();

        record Cat(String name, VaadinIcon icon) {}
        Cat[] cats = new Cat[]{
                new Cat("Summary Sales Report", VaadinIcon.CHART_LINE),
                new Cat("Transaction Report", VaadinIcon.RECORDS),
                new Cat("Payment Report", VaadinIcon.WALLET),
                new Cat("Item Sales Report", VaadinIcon.TAGS),
                new Cat("Daily Sales Report", VaadinIcon.CALENDAR),
                new Cat("Discount Report", VaadinIcon.TAG)
        };

        for (Cat c : cats) {
            Div card = new Div();
            card.addClassName("report-category-card");
            if (c.name().equalsIgnoreCase(selectedCategory)) {
                card.addClassName("active");
            }

            Div iconBox = new Div();
            iconBox.addClassName("report-category-icon-box");
            Icon ic = c.icon().create();
            ic.setSize("22px");
            iconBox.add(ic);

            Span nameSpan = new Span(c.name());
            nameSpan.addClassName("report-category-title");

            card.add(iconBox, nameSpan);
            card.addClickListener(e -> {
                selectedCategory = c.name();
                renderCategoryCards();
            });

            categoriesGrid.add(card);
        }
    }

    private Div createRightSummaryCard() {
        Div card = new Div();
        card.addClassName("figma-report-summary-card");

        H3 title = new H3("Summary Sales Report");
        title.addClassName("report-summary-title");

        dateRangeLabel.addClassName("report-summary-subtitle");
        updateDateRangeLabel();

        // Section REVENUE
        Span revHeader = new Span("REVENUE");
        revHeader.addClassName("report-section-badge");

        Div subtotalRow = createRow("Subtotal", subtotalSpan, false);
        Div discountRow = createRow("Discount", discountSpan, false);
        Div taxRow = createRow("Tax", taxSpan, false);
        Div totalRevRow = createRow("TOTAL", totalRevenueSpan, true);

        // Section PAYMENT
        Span payHeader = new Span("PAYMENT");
        payHeader.addClassName("report-section-badge");

        Div cashRow = createRow("Cash", cashRevenueSpan, false);
        Div qrisRow = createRow("QRIS / Transfer", qrisRevenueSpan, false);
        Div totalPayRow = createRow("TOTAL", totalPaymentSpan, true);

        // CTA Button: Print (Figma node 102:6216)
        Button printBtn = new Button("Print", VaadinIcon.PRINT.create(), e -> {
            ThermalReceiptDialog testDialog = new ThermalReceiptDialog(receiptService, settingService, null, true);
            testDialog.open();
        });
        printBtn.addClassName("report-btn-print");

        card.add(title, dateRangeLabel, revHeader, subtotalRow, discountRow, taxRow, totalRevRow, payHeader, cashRow, qrisRow, totalPayRow, printBtn);
        return card;
    }

    private Div createRow(String label, Span valSpan, boolean isTotal) {
        Div row = new Div();
        row.addClassName("report-summary-row");
        if (isTotal) {
            row.addClassName("total-row");
        }

        Span l = new Span(label);
        valSpan.getStyle().set("text-align", "right");

        row.add(l, valSpan);
        return row;
    }

    private void updateDateRangeLabel() {
        LocalDateTime[] range = reportService.calculateDateRange(selectedPeriod, startDatePicker.getValue(), endDatePicker.getValue());
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d MMM yyyy", new Locale("id", "ID"));
        dateRangeLabel.setText(range[0].format(dtf) + " to " + range[1].format(dtf));
    }

    private void createTransactionGrid() {
        Div gridCard = new Div();
        gridCard.addClassName("modern-card");
        gridCard.getStyle().set("margin-top", "24px").set("border-radius", "8px").set("box-shadow", "0px 4px 4px 2px rgba(0,0,0,0.06)");

        H3 tableTitle = new H3("Rincian Transaksi Pesanan");
        tableTitle.getStyle().set("margin", "0 0 16px 0").set("font-size", "18px").set("font-weight", "600").set("color", "#3949ab").set("font-family", "'Barlow', sans-serif");

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

        gridCard.add(tableTitle, transactionGrid);
        add(gridCard);
    }

    private void refreshReport() {
        try {
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();
            PaymentMethod method = methodSelect.getValue();
            PaymentStatus status = statusSelect.getValue();

            currentOrders = reportService.getOrdersForReport(selectedPeriod, start, end, method, status);
            transactionGrid.setItems(currentOrders);

            BigDecimal total = reportService.calculateTotalRevenue(currentOrders);

            BigDecimal cashTotal = currentOrders.stream()
                    .filter(o -> o.getPaymentMethod() == PaymentMethod.CASH)
                    .map(Order::getPaidAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal nonCashTotal = currentOrders.stream()
                    .filter(o -> o.getPaymentMethod() == PaymentMethod.TRANSFER)
                    .map(Order::getPaidAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            subtotalSpan.setText(ReceiptService.formatRupiah(total));
            discountSpan.setText("Rp 0");
            taxSpan.setText("Rp 0");
            totalRevenueSpan.setText(ReceiptService.formatRupiah(total));

            cashRevenueSpan.setText(ReceiptService.formatRupiah(cashTotal));
            qrisRevenueSpan.setText(ReceiptService.formatRupiah(nonCashTotal));
            totalPaymentSpan.setText(ReceiptService.formatRupiah(cashTotal.add(nonCashTotal)));

            updateDateRangeLabel();
            renderTrendChart(currentOrders);
            updateExcelExportAnchor(currentOrders);
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void renderTrendChart(List<Order> orders) {
        trendChartContainer.removeAll();

        H3 title = new H3("Tren Pendapatan Harian");
        title.getStyle().set("margin", "0 0 1rem 0").set("font-size", "16px").set("color", "#3949ab").set("font-weight", "600").set("font-family", "'Barlow', sans-serif");
        trendChartContainer.add(title);

        if (orders.isEmpty()) {
            Div empty = new Div();
            empty.setText("Belum ada data transaksi pada rentang periode yang dipilih.");
            empty.getStyle().set("color", "#94A3B8").set("padding", "2rem").set("text-align", "center");
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
            amountLabel.getStyle().set("font-size", "0.65rem").set("color", "#7890cd").set("font-weight", "600");

            double ratio = entry.getValue().doubleValue() / max.doubleValue();
            int h = Math.max((int) (ratio * 120), 4);

            Div bar = new Div();
            bar.setWidth("28px");
            bar.setHeight(h + "px");
            bar.getStyle().set("background", "linear-gradient(180deg, #3949ab 0%, #283593 100%)")
                    .set("border-radius", "6px 6px 0 0")
                    .set("box-shadow", "0 2px 4px rgba(57, 73, 171, 0.2)");

            Span dateLabel = new Span(entry.getKey());
            dateLabel.getStyle().set("font-size", "0.7rem").set("margin-top", "4px").set("font-weight", "600").set("color", "#474A56");

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
