package com.example.ui;

import com.example.domain.Role;
import com.example.domain.User;
import com.example.security.AuthenticatedUser;
import com.example.service.SettingService;
import com.example.ui.views.customer.CustomerListView;
import com.example.ui.views.dashboard.DashboardView;
import com.example.ui.views.order.CreateOrderView;
import com.example.ui.views.order.OrderListView;
import com.example.ui.views.report.SalesReportView;
import com.example.ui.views.service.ServiceListView;
import com.example.ui.views.setting.SettingView;
import com.example.ui.views.user.UserManagementView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import jakarta.annotation.security.PermitAll;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@PermitAll
public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private final AuthenticatedUser authenticatedUser;
    private final SettingService settingService;
    private final List<NavButtonRecord> navButtons = new ArrayList<>();

    private record NavButtonRecord(Div container, String pathPrefix, Class<?> targetView) {}

    public MainLayout(AuthenticatedUser authenticatedUser, SettingService settingService) {
        this.authenticatedUser = authenticatedUser;
        this.settingService = settingService;

        setPrimarySection(Section.DRAWER);
        addToNavbar(true, createMobileHeaderContent());
        addDrawerContent();
    }

    private Header createMobileHeaderContent() {
        Header header = new Header();
        header.addClassNames("topbar-container", "mobile-only-header");
        header.setWidthFull();

        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu");
        toggle.getStyle().set("color", "#3949ab");

        String laundryName = settingService.getSetting(SettingService.KEY_LAUNDRY_NAME, "CWB Laundry");
        Span brandBadge = new Span(laundryName + " POS");
        brandBadge.getStyle().set("font-weight", "800")
                .set("font-size", "1.1rem")
                .set("color", "#3949ab");

        HorizontalLayout left = new HorizontalLayout(toggle, brandBadge);
        left.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        left.setSpacing(true);

        Button logoutMobile = new Button(VaadinIcon.SIGN_OUT.create(), e -> authenticatedUser.logout());
        logoutMobile.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

        HorizontalLayout container = new HorizontalLayout(left, logoutMobile);
        container.setWidthFull();
        container.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        container.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        header.add(container);
        return header;
    }

    private void addDrawerContent() {
        Div sidebar = new Div();
        sidebar.addClassName("figma-sidebar");

        // 1. Top: Store Logo squircle
        Div squircleLogo = new Div();
        squircleLogo.addClassName("figma-sidebar-logo");
        Icon storeIcon = VaadinIcon.SHOP.create();
        storeIcon.setSize("26px");
        storeIcon.setColor("#FFFFFF");
        squircleLogo.add(storeIcon);
        squircleLogo.setTooltipText("CWB Laundry POS");
        squircleLogo.addClickListener(e -> UI.getCurrent().navigate(CreateOrderView.class));

        // 2. Middle: Navigation Icons Stack
        Div menuContainer = new Div();
        menuContainer.addClassName("figma-sidebar-menu");

        // Primary: POS Kasir
        addNavButton(menuContainer, VaadinIcon.CART, "POS Kasir", "orders/new", CreateOrderView.class);
        // Dashboard / Graph
        addNavButton(menuContainer, VaadinIcon.DASHBOARD, "Overview Dashboard", "", DashboardView.class);
        // Antrian Order
        addNavButton(menuContainer, VaadinIcon.CLIPBOARD_TEXT, "Antrian Order", "orders", OrderListView.class);
        // Pelanggan
        addNavButton(menuContainer, VaadinIcon.USERS, "Pelanggan", "customers", CustomerListView.class);

        // Owner only
        if (authenticatedUser.isOwner()) {
            addNavButton(menuContainer, VaadinIcon.TAGS, "Paket Layanan", "services", ServiceListView.class);
            addNavButton(menuContainer, VaadinIcon.BAR_CHART, "Laporan Penjualan", "reports", SalesReportView.class);
            addNavButton(menuContainer, VaadinIcon.COG, "Pengaturan Toko", "settings", SettingView.class);
            addNavButton(menuContainer, VaadinIcon.USER_CHECK, "Kelola Staf", "users", UserManagementView.class);
        }

        // 3. Bottom: Logout Button
        Div logoutBtn = new Div();
        logoutBtn.addClassName("figma-logout-btn");
        Icon logoutIcon = VaadinIcon.SIGN_OUT.create();
        logoutIcon.setSize("24px");
        logoutIcon.setColor("#FFFFFF");
        logoutBtn.add(logoutIcon);
        logoutBtn.setTooltipText("Keluar (Logout)");
        logoutBtn.addClickListener(e -> authenticatedUser.logout());

        sidebar.add(squircleLogo, menuContainer, logoutBtn);
        addToDrawer(sidebar);
    }

    private void addNavButton(Div menuContainer, VaadinIcon iconType, String tooltip, String path, Class<?> targetView) {
        Div btn = new Div();
        btn.addClassName("figma-nav-btn");

        Icon icon = iconType.create();
        icon.setSize("24px");
        btn.add(icon);
        btn.setTooltipText(tooltip);

        btn.addClickListener(e -> UI.getCurrent().navigate(targetView));
        menuContainer.add(btn);

        navButtons.add(new NavButtonRecord(btn, path, targetView));
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        String currentPath = event.getLocation().getPath();
        for (NavButtonRecord record : navButtons) {
            boolean active;
            if (record.pathPrefix.isEmpty()) {
                active = currentPath.isEmpty() || currentPath.equals("dashboard");
            } else if (record.pathPrefix.equals("orders/new")) {
                active = currentPath.equals("orders/new");
            } else if (record.pathPrefix.equals("orders")) {
                active = currentPath.startsWith("orders") && !currentPath.equals("orders/new");
            } else {
                active = currentPath.startsWith(record.pathPrefix);
            }

            if (active) {
                record.container.addClassName("active");
            } else {
                record.container.removeClassName("active");
            }
        }
    }
}
