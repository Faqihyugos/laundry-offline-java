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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@Route(value = "customers", layout = MainLayout.class)
@PageTitle("Pelanggan | Laundry POS")
@RolesAllowed({"OWNER", "CASHIER"})
public class CustomerListView extends VerticalLayout {

    private final CustomerService customerService;
    private final Grid<Customer> grid = new Grid<>(Customer.class, false);
    private final Div mobileCardContainer = new Div();
    private final TextField searchField = new TextField();

    public CustomerListView(CustomerService customerService) {
        this.customerService = customerService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("max-width", "1280px").set("margin", "0 auto");

        createHeader();
        createGridAndCards();
        refreshGrid();
    }

    private void createHeader() {
        H2 title = new H2("Manajemen Pelanggan");
        title.getStyle().set("margin", "0").set("color", "#7B2D8E").set("font-weight", "800");

        Paragraph subtitle = new Paragraph("Kelola database pelanggan, riwayat transaksi, dan komunikasi WhatsApp/Telepon.");
        subtitle.getStyle().set("margin", "4px 0 0.5rem 0").set("color", "#6C667A").set("font-size", "0.9rem");

        searchField.setPlaceholder("Cari nama atau nomor telepon...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setValueChangeTimeout(300);
        searchField.addValueChangeListener(e -> refreshGrid());
        searchField.setWidth("340px");

        Button addBtn = new Button("Tambah Pelanggan", VaadinIcon.PLUS.create(), e -> openCustomerDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addBtn.getStyle().set("background-color", "#7B2D8E");

        Div toolbar = new Div();
        toolbar.addClassName("responsive-toolbar");
        toolbar.add(searchField, addBtn);

        add(title, subtitle, toolbar);
    }

    private void createGridAndCards() {
        grid.setSizeFull();
        grid.addClassName("desktop-only");

        grid.addColumn(Customer::getName).setHeader("Nama Lengkap").setSortable(true).setAutoWidth(true);
        grid.addColumn(Customer::getPhone).setHeader("No. Telepon").setAutoWidth(true);
        grid.addColumn(Customer::getAddress).setHeader("Alamat").setFlexGrow(2);
        grid.addColumn(c -> c.getOrders() != null ? c.getOrders().size() + " pesanan" : "0 pesanan")
                .setHeader("Total Order").setAutoWidth(true);

        grid.addComponentColumn(customer -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            String waUrl = CustomerService.buildWhatsAppUrl(customer.getPhone(),
                    "Halo " + customer.getName() + ", kami dari Laundry Bersih Wangi.");
            Anchor waLink = new Anchor(waUrl, "");
            waLink.setTarget("_blank");
            Button waBtn = new Button(VaadinIcon.CHAT.create());
            waBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
            waBtn.setTooltipText("Chat WhatsApp");
            waLink.add(waBtn);

            Anchor telLink = new Anchor("tel:" + customer.getPhone(), "");
            Button callBtn = new Button(VaadinIcon.PHONE.create());
            callBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            callBtn.setTooltipText("Telepon");
            telLink.add(callBtn);

            Button historyBtn = new Button(VaadinIcon.TIME_BACKWARD.create(), e -> {
                UI.getCurrent().navigate(CustomerDetailView.class, customer.getId());
            });
            historyBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            historyBtn.setTooltipText("Riwayat Order");

            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openCustomerDialog(customer));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            editBtn.setTooltipText("Edit Pelanggan");

            Button delBtn = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(customer));
            delBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            delBtn.setTooltipText("Hapus Pelanggan");

            actions.add(waLink, telLink, historyBtn, editBtn, delBtn);
            return actions;
        }).setHeader("Aksi").setAutoWidth(true);

        mobileCardContainer.addClassName("mobile-only");
        mobileCardContainer.setWidthFull();

        add(grid, mobileCardContainer);
    }

    private void refreshGrid() {
        List<Customer> customers = customerService.search(searchField.getValue());
        grid.setItems(customers);
        renderMobileCards(customers);
    }

    private void renderMobileCards(List<Customer> customers) {
        mobileCardContainer.removeAll();

        if (customers.isEmpty()) {
            Div emptyMsg = new Div();
            emptyMsg.setText("Tidak ada data pelanggan yang ditemukan.");
            emptyMsg.getStyle().set("color", "#9C96A8").set("padding", "2rem").set("text-align", "center");
            mobileCardContainer.add(emptyMsg);
            return;
        }

        for (Customer customer : customers) {
            Div card = new Div();
            card.addClassName("mobile-order-card");

            Div row1 = new Div();
            row1.addClassName("mobile-order-card-header");
            Span name = new Span(customer.getName());
            name.getStyle().set("font-weight", "800").set("font-size", "1rem");

            Span orderCount = new Span((customer.getOrders() != null ? customer.getOrders().size() : 0) + " pesanan");
            orderCount.addClassName("status-badge");
            orderCount.addClassName("badge-role-kasir");
            row1.add(name, orderCount);

            Div row2 = new Div();
            row2.getStyle().set("display", "flex").set("justify-content", "space-between").set("align-items", "center");
            Span phone = new Span(customer.getPhone());
            phone.getStyle().set("font-size", "0.9rem").set("color", "#6C667A");

            HorizontalLayout quickComm = new HorizontalLayout();
            quickComm.setSpacing(true);

            String waUrl = CustomerService.buildWhatsAppUrl(customer.getPhone(),
                    "Halo " + customer.getName() + ", kami dari Laundry Bersih Wangi.");
            Anchor waLink = new Anchor(waUrl, "");
            waLink.setTarget("_blank");
            Button waBtn = new Button(VaadinIcon.CHAT.create());
            waBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
            waLink.add(waBtn);

            Anchor telLink = new Anchor("tel:" + customer.getPhone(), "");
            Button callBtn = new Button(VaadinIcon.PHONE.create());
            callBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            telLink.add(callBtn);

            quickComm.add(waLink, telLink);
            row2.add(phone, quickComm);

            if (customer.getAddress() != null && !customer.getAddress().isEmpty()) {
                Paragraph addr = new Paragraph(customer.getAddress());
                addr.getStyle().set("margin", "0").set("color", "#9C96A8").set("font-size", "0.8rem");
                card.add(addr);
            }

            Div row3 = new Div();
            row3.getStyle().set("display", "flex").set("justify-content", "flex-end").set("gap", "8px").set("margin-top", "6px");

            Button histBtn = new Button("Riwayat", VaadinIcon.TIME_BACKWARD.create(), e -> {
                UI.getCurrent().navigate(CustomerDetailView.class, customer.getId());
            });
            histBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

            Button editBtn = new Button("Edit", VaadinIcon.EDIT.create(), e -> openCustomerDialog(customer));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

            Button delBtn = new Button("Hapus", VaadinIcon.TRASH.create(), e -> confirmDelete(customer));
            delBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

            row3.add(histBtn, editBtn, delBtn);

            card.add(row1, row2, row3);
            mobileCardContainer.add(card);
        }
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
