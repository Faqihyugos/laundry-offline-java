package com.example.ui.views.customer;

import com.example.domain.Customer;
import com.example.service.CustomerService;
import com.example.ui.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "customers", layout = MainLayout.class)
@PageTitle("Pelanggan | Laundry POS")
@RolesAllowed({"OWNER", "CASHIER"})
public class CustomerListView extends VerticalLayout {

    private final CustomerService customerService;
    private final Grid<Customer> grid = new Grid<>(Customer.class, false);
    private final TextField searchField = new TextField();

    public CustomerListView(CustomerService customerService) {
        this.customerService = customerService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        createHeader();
        createGrid();
        refreshGrid();
    }

    private void createHeader() {
        H2 title = new H2("Manajemen Pelanggan");
        title.getStyle().set("margin", "0").set("color", "#7B2D8E");

        Paragraph subtitle = new Paragraph("Kelola database pelanggan, riwayat transaksi, dan komunikasi WhatsApp/Telepon.");
        subtitle.getStyle().set("margin", "0 0 1rem 0").set("color", "#6B6B7B");

        searchField.setPlaceholder("Cari nama atau nomor telepon...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setValueChangeTimeout(300);
        searchField.addValueChangeListener(e -> refreshGrid());
        searchField.setWidth("320px");

        Button addBtn = new Button("Tambah Pelanggan", VaadinIcon.PLUS.create(), e -> openCustomerDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addBtn.getStyle().set("background-color", "#7B2D8E");

        HorizontalLayout toolbar = new HorizontalLayout(searchField, addBtn);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        add(title, subtitle, toolbar);
    }

    private void createGrid() {
        grid.setSizeFull();

        grid.addColumn(Customer::getName).setHeader("Nama Lengkap").setSortable(true).setAutoWidth(true);
        grid.addColumn(Customer::getPhone).setHeader("No. Telepon").setAutoWidth(true);
        grid.addColumn(Customer::getAddress).setHeader("Alamat").setFlexGrow(2);
        grid.addColumn(c -> c.getOrders() != null ? c.getOrders().size() + " pesanan" : "0 pesanan")
                .setHeader("Total Order").setAutoWidth(true);

        grid.addComponentColumn(customer -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            // WhatsApp Quick Action
            String waUrl = CustomerService.buildWhatsAppUrl(customer.getPhone(),
                    "Halo " + customer.getName() + ", kami dari Laundry Bersih Wangi.");
            Anchor waLink = new Anchor(waUrl, "");
            waLink.setTarget("_blank");
            Button waBtn = new Button(VaadinIcon.CHAT.create());
            waBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
            waBtn.setTooltipText("Chat WhatsApp");
            waLink.add(waBtn);

            // Phone Call Quick Action
            Anchor telLink = new Anchor("tel:" + customer.getPhone(), "");
            Button callBtn = new Button(VaadinIcon.PHONE.create());
            callBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            callBtn.setTooltipText("Telepon");
            telLink.add(callBtn);

            // Order History Button
            Button historyBtn = new Button(VaadinIcon.TIME_BACKWARD.create(), e -> {
                UI.getCurrent().navigate(CustomerDetailView.class, customer.getId());
            });
            historyBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            historyBtn.setTooltipText("Riwayat Order");

            // Edit Button
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openCustomerDialog(customer));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            editBtn.setTooltipText("Edit Pelanggan");

            // Delete Button
            Button delBtn = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(customer));
            delBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            delBtn.setTooltipText("Hapus Pelanggan");

            actions.add(waLink, telLink, historyBtn, editBtn, delBtn);
            return actions;
        }).setHeader("Aksi").setAutoWidth(true);

        add(grid);
    }

    private void refreshGrid() {
        grid.setItems(customerService.search(searchField.getValue()));
    }

    private void openCustomerDialog(Customer customer) {
        boolean isEdit = customer != null;
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isEdit ? "Edit Data Pelanggan" : "Tambah Pelanggan Baru");

        TextField nameField = new TextField("Nama Lengkap");
        nameField.setRequired(true);
        nameField.setWidthFull();

        TextField phoneField = new TextField("Nomor Telepon (WhatsApp)");
        phoneField.setRequired(true);
        phoneField.setPlaceholder("Contoh: 081234567890");
        phoneField.setWidthFull();

        TextArea addressField = new TextArea("Alamat Lengkap");
        addressField.setPlaceholder("Alamat pengiriman / penjemputan");
        addressField.setWidthFull();

        if (isEdit) {
            nameField.setValue(customer.getName() != null ? customer.getName() : "");
            phoneField.setValue(customer.getPhone() != null ? customer.getPhone() : "");
            addressField.setValue(customer.getAddress() != null ? customer.getAddress() : "");
        }

        VerticalLayout form = new VerticalLayout(nameField, phoneField, addressField);
        form.setPadding(false);
        form.setSpacing(true);
        dialog.add(form);

        Button saveBtn = new Button(isEdit ? "Simpan Perubahan" : "Simpan Pelanggan", e -> {
            try {
                if (isEdit) {
                    customerService.updateCustomer(customer.getId(), nameField.getValue(), phoneField.getValue(), addressField.getValue());
                    showNotification("Data pelanggan berhasil diperbarui", NotificationVariant.LUMO_SUCCESS);
                } else {
                    customerService.createCustomer(nameField.getValue(), phoneField.getValue(), addressField.getValue());
                    showNotification("Pelanggan baru berhasil ditambahkan", NotificationVariant.LUMO_SUCCESS);
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

    private void confirmDelete(Customer customer) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Hapus Pelanggan");

        confirmDialog.add(new Paragraph("Apakah Anda yakin ingin menghapus pelanggan '" + customer.getName() + "'?"));

        Button deleteBtn = new Button("Hapus", e -> {
            try {
                customerService.deleteCustomer(customer.getId());
                showNotification("Pelanggan berhasil dihapus", NotificationVariant.LUMO_SUCCESS);
                confirmDialog.close();
                refreshGrid();
            } catch (Exception ex) {
                showNotification(ex.getMessage(), NotificationVariant.LUMO_ERROR);
                confirmDialog.close();
            }
        });
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button cancelBtn = new Button("Batal", e -> confirmDialog.close());
        confirmDialog.getFooter().add(cancelBtn, deleteBtn);

        confirmDialog.open();
    }

    private void showNotification(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(variant);
    }
}
