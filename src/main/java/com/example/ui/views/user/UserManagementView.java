package com.example.ui.views.user;

import com.example.domain.Role;
import com.example.domain.User;
import com.example.security.AuthenticatedUser;
import com.example.service.UserService;
import com.example.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("Kelola User | Laundry POS")
@RolesAllowed("OWNER")
public class UserManagementView extends VerticalLayout {

    private final UserService userService;
    private final AuthenticatedUser authenticatedUser;
    private final Grid<User> grid = new Grid<>(User.class, false);
    private final Div mobileCardContainer = new Div();
    private final TextField searchField = new TextField();

    public UserManagementView(UserService userService, AuthenticatedUser authenticatedUser) {
        this.userService = userService;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("max-width", "1360px").set("margin", "0 auto");

        createHeader();
        createGridAndCards();
        refreshGrid();
    }

    private void createHeader() {
        Div headerContainer = new Div();
        headerContainer.setWidthFull();

        Div topRow = new Div();
        topRow.addClassName("responsive-toolbar");

        Div titleGroup = new Div();
        H2 title = new H2("Manajemen Pengguna & Hak Akses");
        title.getStyle().set("margin", "0")
                .set("color", "var(--text-main)")
                .set("font-size", "1.6rem")
                .set("font-weight", "800")
                .set("letter-spacing", "-0.02em");

        Paragraph subtitle = new Paragraph("Kelola akun staf (Kasir dan Owner) dengan kontrol akses berbasis peran.");
        subtitle.getStyle().set("margin", "4px 0 0 0")
                .set("color", "var(--text-muted)")
                .set("font-size", "0.88rem");
        titleGroup.add(title, subtitle);

        searchField.setPlaceholder("Cari nama staf atau username...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setWidth("280px");
        searchField.addValueChangeListener(e -> refreshGrid());

        Button addBtn = new Button("Tambah User", VaadinIcon.PLUS.create(), e -> openUserDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addBtn.getStyle().set("background", "var(--primary-gradient)")
                .set("box-shadow", "0 4px 12px rgba(79, 70, 229, 0.25)")
                .set("font-weight", "700")
                .set("border-radius", "10px");

        HorizontalLayout actions = new HorizontalLayout(searchField, addBtn);
        actions.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        topRow.add(titleGroup, actions);
        headerContainer.add(topRow);

        add(headerContainer);
    }

    private void createGridAndCards() {
        Div cardWrapper = new Div();
        cardWrapper.addClassName("modern-card");
        cardWrapper.getStyle().set("padding", "0").set("overflow", "hidden");

        grid.setSizeFull();
        grid.addClassName("desktop-only");

        grid.addColumn(User::getUsername).setHeader("Username").setAutoWidth(true);
        grid.addColumn(User::getFullName).setHeader("Nama Lengkap").setFlexGrow(2);

        grid.addComponentColumn(user -> {
            Span badge = new Span(user.getRole().name());
            badge.addClassName("status-badge");
            badge.addClassName(user.getRole() == Role.OWNER ? "badge-role-owner" : "badge-role-kasir");
            return badge;
        }).setHeader("Role").setAutoWidth(true);

        grid.addComponentColumn(user -> {
            Span badge = new Span(user.isActive() ? "Aktif" : "Nonaktif");
            badge.addClassName("status-badge");
            badge.addClassName(user.isActive() ? "badge-selesai" : "badge-belum-lunas");
            return badge;
        }).setHeader("Status").setAutoWidth(true);

        grid.addComponentColumn(user -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            Long currentUserId = authenticatedUser.get().map(User::getId).orElse(null);

            Button editBtn = new Button("Edit", VaadinIcon.EDIT.create(), e -> openUserDialog(user));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            editBtn.setTooltipText("Edit User");

            if (user.isActive()) {
                Button deactBtn = new Button("Nonaktifkan", e -> {
                    try {
                        userService.deactivateUser(user.getId(), currentUserId);
                        showNotification("Akun berhasil dinonaktifkan", NotificationVariant.LUMO_SUCCESS);
                        refreshGrid();
                    } catch (Exception ex) {
                        showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
                    }
                });
                deactBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
                if (user.getId().equals(currentUserId)) {
                    deactBtn.setEnabled(false);
                    deactBtn.setTooltipText("Tidak dapat menonaktifkan akun sendiri");
                }
                actions.add(editBtn, deactBtn);
            } else {
                Button actBtn = new Button("Aktifkan", e -> {
                    userService.activateUser(user.getId());
                    showNotification("Akun berhasil diaktifkan kembali", NotificationVariant.LUMO_SUCCESS);
                    refreshGrid();
                });
                actBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
                actions.add(editBtn, actBtn);
            }

            return actions;
        }).setHeader("Aksi").setAutoWidth(true);

        cardWrapper.add(grid);

        mobileCardContainer.addClassName("mobile-only");
        mobileCardContainer.setWidthFull();

        add(cardWrapper, mobileCardContainer);
    }

    private void refreshGrid() {
        String filter = searchField.getValue() != null ? searchField.getValue().trim().toLowerCase() : "";
        List<User> users = userService.findAll().stream()
                .filter(u -> filter.isEmpty() ||
                        u.getUsername().toLowerCase().contains(filter) ||
                        (u.getFullName() != null && u.getFullName().toLowerCase().contains(filter)))
                .toList();
        grid.setItems(users);
        renderMobileCards(users);
    }

    private void renderMobileCards(List<User> users) {
        mobileCardContainer.removeAll();
        Long currentUserId = authenticatedUser.get().map(User::getId).orElse(null);

        for (User user : users) {
            Div card = new Div();
            card.addClassName("mobile-order-card");

            Div row1 = new Div();
            row1.addClassName("mobile-order-card-header");
            Span name = new Span(user.getFullName());
            name.getStyle().set("font-weight", "800").set("font-size", "1rem");

            Span roleBadge = new Span(user.getRole().name());
            roleBadge.addClassNames("status-badge", user.getRole() == Role.OWNER ? "badge-role-owner" : "badge-role-kasir");
            row1.add(name, roleBadge);

            Div row2 = new Div();
            row2.getStyle().set("display", "flex").set("justify-content", "space-between").set("align-items", "center");
            Span username = new Span("@" + user.getUsername());
            username.getStyle().set("color", "#6C667A").set("font-size", "0.85rem");

            Span statusBadge = new Span(user.isActive() ? "Aktif" : "Nonaktif");
            statusBadge.addClassNames("status-badge", user.isActive() ? "badge-selesai" : "badge-belum-lunas");
            row2.add(username, statusBadge);

            Div row3 = new Div();
            row3.getStyle().set("display", "flex").set("justify-content", "flex-end").set("gap", "8px").set("margin-top", "6px");

            Button editBtn = new Button("Edit", VaadinIcon.EDIT.create(), e -> openUserDialog(user));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

            row3.add(editBtn);

            if (user.isActive()) {
                Button deactBtn = new Button("Nonaktifkan", e -> {
                    try {
                        userService.deactivateUser(user.getId(), currentUserId);
                        showNotification("Akun berhasil dinonaktifkan", NotificationVariant.LUMO_SUCCESS);
                        refreshGrid();
                    } catch (Exception ex) {
                        showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
                    }
                });
                deactBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
                if (user.getId().equals(currentUserId)) {
                    deactBtn.setEnabled(false);
                }
                row3.add(deactBtn);
            } else {
                Button actBtn = new Button("Aktifkan", e -> {
                    userService.activateUser(user.getId());
                    showNotification("Akun berhasil diaktifkan kembali", NotificationVariant.LUMO_SUCCESS);
                    refreshGrid();
                });
                actBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
                row3.add(actBtn);
            }

            card.add(row1, row2, row3);
            mobileCardContainer.add(card);
        }
    }

    private void openUserDialog(User user) {
        boolean isEdit = user != null;
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isEdit ? "Edit User" : "Tambah User Baru");

        TextField usernameField = new TextField("Username");
        usernameField.setRequired(true);
        usernameField.setWidthFull();

        TextField fullNameField = new TextField("Nama Lengkap");
        fullNameField.setRequired(true);
        fullNameField.setWidthFull();

        Select<Role> roleSelect = new Select<>();
        roleSelect.setLabel("Peran / Role");
        roleSelect.setItems(Role.values());
        roleSelect.setValue(Role.CASHIER);
        roleSelect.setWidthFull();

        PasswordField passwordField = new PasswordField(isEdit ? "Password Baru (Kosongkan jika tidak diubah)" : "Password");
        passwordField.setWidthFull();
        if (!isEdit) passwordField.setRequired(true);

        PasswordField confirmPasswordField = new PasswordField(isEdit ? "Konfirmasi Password Baru" : "Konfirmasi Password");
        confirmPasswordField.setWidthFull();
        if (!isEdit) confirmPasswordField.setRequired(true);

        if (isEdit) {
            usernameField.setValue(user.getUsername());
            usernameField.setEnabled(false);
            fullNameField.setValue(user.getFullName());
            roleSelect.setValue(user.getRole());
        }

        VerticalLayout form = new VerticalLayout(usernameField, fullNameField, roleSelect, passwordField, confirmPasswordField);
        form.setPadding(false);
        form.setSpacing(true);
        dialog.add(form);

        Button saveBtn = new Button(isEdit ? "Simpan Perubahan" : "Simpan User", e -> {
            try {
                Long currentUserId = authenticatedUser.get().map(User::getId).orElse(null);
                if (isEdit) {
                    userService.updateUser(user.getId(), fullNameField.getValue(), roleSelect.getValue(),
                            passwordField.getValue(), confirmPasswordField.getValue(), currentUserId);
                    showNotification("Data user berhasil diperbarui", NotificationVariant.LUMO_SUCCESS);
                } else {
                    userService.createUser(usernameField.getValue(), fullNameField.getValue(),
                            passwordField.getValue(), confirmPasswordField.getValue(), roleSelect.getValue());
                    showNotification("User baru berhasil dibuat", NotificationVariant.LUMO_SUCCESS);
                }
                dialog.close();
                refreshGrid();
            } catch (Exception ex) {
                showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.getStyle().set("background", "var(--primary-gradient)")
                .set("box-shadow", "0 4px 12px rgba(79, 70, 229, 0.25)")
                .set("border-radius", "10px");

        Button cancelBtn = new Button("Batal", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);

        dialog.open();
    }

    private void showNotification(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
