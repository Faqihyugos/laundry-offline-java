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

import java.util.Optional;

@Route(value = "users", layout = MainLayout.class)
@PageTitle("Kelola User | Laundry POS")
@RolesAllowed("OWNER")
public class UserManagementView extends VerticalLayout {

    private final UserService userService;
    private final AuthenticatedUser authenticatedUser;
    private final Grid<User> grid = new Grid<>(User.class, false);

    public UserManagementView(UserService userService, AuthenticatedUser authenticatedUser) {
        this.userService = userService;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        createHeader();
        createGrid();
        refreshGrid();
    }

    private void createHeader() {
        H2 title = new H2("Manajemen Pengguna & Hak Akses");
        title.getStyle().set("margin", "0").set("color", "#7B2D8E");

        Paragraph subtitle = new Paragraph("Kelola akun staf (Kasir dan Owner) dengan kontrol akses berbasis peran.");
        subtitle.getStyle().set("margin", "0 0 1rem 0").set("color", "#6B6B7B");

        Button addBtn = new Button("Tambah User", VaadinIcon.PLUS.create(), e -> openUserDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addBtn.getStyle().set("background-color", "#7B2D8E");

        HorizontalLayout toolbar = new HorizontalLayout(title, addBtn);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        add(toolbar, subtitle);
    }

    private void createGrid() {
        grid.setSizeFull();

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

            // Edit button
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openUserDialog(user));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            editBtn.setTooltipText("Edit User");

            // Deactivate / Activate button
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
                deactBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
                // Disable if own account
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
                actBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
                actions.add(editBtn, actBtn);
            }

            return actions;
        }).setHeader("Aksi").setAutoWidth(true);

        add(grid);
    }

    private void refreshGrid() {
        grid.setItems(userService.findAll());
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
            usernameField.setEnabled(false); // Username immutable on edit
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
        saveBtn.getStyle().set("background-color", "#7B2D8E");

        Button cancelBtn = new Button("Batal", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);

        dialog.open();
    }

    private void showNotification(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
