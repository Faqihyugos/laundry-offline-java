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
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import jakarta.annotation.security.PermitAll;

import java.util.Optional;

@PermitAll
public class MainLayout extends AppLayout {

    private final AuthenticatedUser authenticatedUser;
    private final SettingService settingService;

    public MainLayout(AuthenticatedUser authenticatedUser, SettingService settingService) {
        this.authenticatedUser = authenticatedUser;
        this.settingService = settingService;

        setPrimarySection(Section.DRAWER);
        addToNavbar(true, createHeaderContent());
        addDrawerContent();
    }

    private Header createHeaderContent() {
        Header header = new Header();
        header.addClassNames("header-layout");
        header.setWidthFull();

        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu navigasi");
        toggle.getStyle().set("color", "#7B2D8E");

        String laundryName = settingService.getSetting(SettingService.KEY_LAUNDRY_NAME, SettingService.DEFAULT_LAUNDRY_NAME);
        H1 title = new H1(laundryName);
        title.addClassName("header-brand-title");

        HorizontalLayout left = new HorizontalLayout(toggle, title);
        left.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        left.setSpacing(true);

        HorizontalLayout userSection = new HorizontalLayout();
        userSection.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        userSection.setSpacing(true);

        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            Span userBadge = new Span(user.getFullName() + " (" + user.getRole().name() + ")");
            userBadge.addClassName(user.getRole() == Role.OWNER ? "badge-role-owner" : "badge-role-kasir");
            userBadge.addClassName("status-badge");
            userBadge.getStyle().set("white-space", "nowrap");

            Button logoutBtn = new Button("Keluar", VaadinIcon.SIGN_OUT.create(), e -> authenticatedUser.logout());
            logoutBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            logoutBtn.getStyle().set("cursor", "pointer");
            logoutBtn.setTooltipText("Logout dari sistem");

            userSection.add(userBadge, logoutBtn);
        }

        HorizontalLayout container = new HorizontalLayout(left, userSection);
        container.setWidthFull();
        container.addClassName("header-container");
        container.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        container.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        header.add(container);
        return header;
    }

    private void addDrawerContent() {
        Div brandHeader = new Div();
        brandHeader.getStyle().set("padding", "1.25rem 1rem")
                .set("border-bottom", "1px solid var(--border-subtle)")
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "10px");

        Span logoIcon = new Span("🧺");
        logoIcon.getStyle().set("font-size", "1.5rem");

        Div brandTexts = new Div();
        Span appTitle = new Span("POS Laundry");
        appTitle.getStyle().set("font-weight", "800")
                .set("font-size", "1.1rem")
                .set("color", "#7B2D8E")
                .set("display", "block");
        Span appSub = new Span("Point-of-Sale & Antrian");
        appSub.getStyle().set("font-size", "0.75rem").set("color", "#6C667A");
        brandTexts.add(appTitle, appSub);

        brandHeader.add(logoIcon, brandTexts);

        SideNav nav = new SideNav();

        nav.addItem(new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create()));
        nav.addItem(new SideNavItem("Order Baru", CreateOrderView.class, VaadinIcon.PLUS_CIRCLE.create()));
        nav.addItem(new SideNavItem("Daftar Order", OrderListView.class, VaadinIcon.FILE_TEXT.create()));
        nav.addItem(new SideNavItem("Pelanggan", CustomerListView.class, VaadinIcon.USERS.create()));

        if (authenticatedUser.isOwner()) {
            nav.addItem(new SideNavItem("Paket Layanan", ServiceListView.class, VaadinIcon.CLIPBOARD_CHECK.create()));
            nav.addItem(new SideNavItem("Laporan Penjualan", SalesReportView.class, VaadinIcon.CHART.create()));
            nav.addItem(new SideNavItem("Pengaturan Toko", SettingView.class, VaadinIcon.COG.create()));
            nav.addItem(new SideNavItem("Kelola User", UserManagementView.class, VaadinIcon.USER_CHECK.create()));
        }

        Scroller scroller = new Scroller(nav);
        scroller.setClassName("nav-scroller");
        scroller.getStyle().set("padding", "0.75rem 0.5rem");

        Div footer = new Div();
        footer.getStyle().set("padding", "1rem")
                .set("font-size", "0.75rem")
                .set("color", "#9C96A8")
                .set("text-align", "center")
                .set("border-top", "1px solid var(--border-subtle)");
        footer.setText("Laundry App v1.0 • Offline Ready");

        VerticalLayout drawerLayout = new VerticalLayout(brandHeader, scroller, footer);
        drawerLayout.setSizeFull();
        drawerLayout.setPadding(false);
        drawerLayout.setSpacing(false);
        drawerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        addToDrawer(drawerLayout);
    }
}
