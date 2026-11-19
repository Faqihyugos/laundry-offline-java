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
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.textfield.TextField;
import jakarta.annotation.security.PermitAll;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
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
        header.addClassNames("topbar-container");
        header.setWidthFull();

        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu");
        toggle.getStyle().set("color", "#6D28D9");

        TextField searchInput = new TextField();
        searchInput.setPlaceholder("Cari layanan, order, pelanggan... (Ctrl+K)");
        searchInput.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchInput.setClearButtonVisible(true);
        searchInput.addClassName("desktop-only");
        searchInput.getStyle().set("width", "340px");
        searchInput.addKeyPressListener(e -> {
            if ("Enter".equals(e.getKey().getKeys().get(0))) {
                UI.getCurrent().navigate(OrderListView.class);
            }
        });

        HorizontalLayout left = new HorizontalLayout(toggle, searchInput);
        left.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        left.setSpacing(true);

        HorizontalLayout right = new HorizontalLayout();
        right.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        right.setSpacing(true);

        LocalDate today = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", new Locale("id", "ID"));
        HorizontalLayout dateBox = new HorizontalLayout(VaadinIcon.CALENDAR.create(), new Span(today.format(dtf)));
        dateBox.addClassName("desktop-only");
        dateBox.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        dateBox.getStyle().set("font-size", "0.82rem").set("color", "#64748B").set("font-weight", "500").set("gap", "6px");

        Span statusPill = new Span("● Online");
        statusPill.addClassNames("status-badge", "badge-selesai", "desktop-only");

        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();
            String initial = user.getFullName() != null && !user.getFullName().isEmpty()
                    ? user.getFullName().substring(0, 1).toUpperCase() : "U";

            Div profileChip = new Div();
            profileChip.addClassName("topbar-profile-chip");

            Div avatar = new Div();
            avatar.addClassName("topbar-avatar");
            avatar.setText(initial);

            Div userMeta = new Div();
            userMeta.addClassName("desktop-only");
            Span userName = new Span(user.getFullName());
            userName.getStyle().set("font-weight", "700").set("font-size", "0.85rem").set("display", "block").set("line-height", "1.2");
            Span userRole = new Span(user.getRole() == Role.OWNER ? "Owner" : "Kasir");
            userRole.getStyle().set("font-size", "0.72rem").set("color", "#64748B");
            userMeta.add(userName, userRole);

            profileChip.add(avatar, userMeta);

            Button logoutBtn = new Button(VaadinIcon.SIGN_OUT.create(), e -> authenticatedUser.logout());
            logoutBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            logoutBtn.setTooltipText("Keluar");

            right.add(dateBox, statusPill, profileChip, logoutBtn);
        }

        HorizontalLayout container = new HorizontalLayout(left, right);
        container.setWidthFull();
        container.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        container.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        header.add(container);
        return header;
    }

    private void addDrawerContent() {
        Div brandHeader = new Div();
        brandHeader.addClassName("sidebar-brand-box");

        Div squircleLogo = new Div();
        squircleLogo.addClassName("sidebar-squircle-logo");
        Icon washerIcon = VaadinIcon.TASKS.create();
        washerIcon.setSize("20px");
        washerIcon.setColor("#FFFFFF");
        squircleLogo.add(washerIcon);

        Div brandTexts = new Div();
        brandTexts.getStyle().set("display", "flex").set("flex-direction", "column");
        String laundryName = settingService.getSetting(SettingService.KEY_LAUNDRY_NAME, SettingService.DEFAULT_LAUNDRY_NAME);
        Span appTitle = new Span(laundryName);
        appTitle.addClassName("sidebar-brand-title");

        Span appSub = new Span("Point-of-Sale System");
        appSub.addClassName("sidebar-brand-sub");
        brandTexts.add(appTitle, appSub);

        brandHeader.add(squircleLogo, brandTexts);

        SideNav nav = new SideNav();

        nav.addItem(new SideNavItem("Overview", DashboardView.class, VaadinIcon.DASHBOARD.create()));
        nav.addItem(new SideNavItem("POS Kasir", CreateOrderView.class, VaadinIcon.CART.create()));
        nav.addItem(new SideNavItem("Antrian Order", OrderListView.class, VaadinIcon.CLIPBOARD_TEXT.create()));
        nav.addItem(new SideNavItem("Pelanggan", CustomerListView.class, VaadinIcon.USERS.create()));

        if (authenticatedUser.isOwner()) {
            nav.addItem(new SideNavItem("Paket Layanan", ServiceListView.class, VaadinIcon.TAGS.create()));
            nav.addItem(new SideNavItem("Laporan Penjualan", SalesReportView.class, VaadinIcon.CHART_LINE.create()));
            nav.addItem(new SideNavItem("Pengaturan Toko", SettingView.class, VaadinIcon.COG.create()));
            nav.addItem(new SideNavItem("Kelola Staf", UserManagementView.class, VaadinIcon.USER_CHECK.create()));
        }

        Scroller scroller = new Scroller(nav);
        scroller.setClassName("nav-scroller");
        scroller.getStyle().set("padding", "0.75rem 0.5rem");

        Div shiftCard = new Div();
        shiftCard.addClassName("sidebar-shift-card");
        Span shiftStatus = new Span("Kasir Ready");
        shiftStatus.getStyle().set("font-weight", "700").set("color", "#6D28D9").set("display", "block");
        Span offlineReady = new Span("Offline-First Persistent DB");
        offlineReady.getStyle().set("font-size", "0.72rem").set("color", "#64748B").set("display", "block").set("margin-top", "2px");
        shiftCard.add(shiftStatus, offlineReady);

        Div footer = new Div();
        footer.getStyle().set("padding", "0.75rem 1rem")
                .set("font-size", "0.72rem")
                .set("color", "#94A3B8")
                .set("text-align", "center")
                .set("border-top", "1px solid var(--border-subtle)");
        footer.setText("Laundry App v2.0 • Offline Ready");

        VerticalLayout drawerLayout = new VerticalLayout(brandHeader, scroller, shiftCard, footer);
        drawerLayout.setSizeFull();
        drawerLayout.setPadding(false);
        drawerLayout.setSpacing(false);
        drawerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        addToDrawer(drawerLayout);
    }
}
