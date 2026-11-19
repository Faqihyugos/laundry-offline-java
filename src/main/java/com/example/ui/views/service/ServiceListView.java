package com.example.ui.views.service;

import com.example.domain.LaundryService;
import com.example.domain.ServiceUnit;
import com.example.service.ReceiptService;
import com.example.service.ServicePackageService;
import com.example.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
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
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.util.List;

@Route(value = "services", layout = MainLayout.class)
@PageTitle("Paket Layanan | Laundry POS")
@RolesAllowed("OWNER")
public class ServiceListView extends VerticalLayout {

    private final ServicePackageService servicePackageService;
    private final Grid<LaundryService> grid = new Grid<>(LaundryService.class, false);
    private final Div mobileCardContainer = new Div();
    private final TextField searchField = new TextField();

    public ServiceListView(ServicePackageService servicePackageService) {
        this.servicePackageService = servicePackageService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("max-width", "1280px").set("margin", "0 auto");

        createHeader();
        createGridAndCards();
        refreshGrid();
    }

    private void createHeader() {
        H2 title = new H2("Manajemen Paket Layanan");
        title.getStyle().set("margin", "0")
                .set("color", "var(--text-main)")
                .set("font-size", "1.6rem")
                .set("font-weight", "800")
                .set("letter-spacing", "-0.02em");

        Paragraph subtitle = new Paragraph("Kelola katalog tarif cucian, satuan (kg / pcs), dan estimasi pengerjaan.");
        subtitle.getStyle().set("margin", "4px 0 0.5rem 0").set("color", "var(--text-muted)").set("font-size", "0.88rem");

        searchField.setPlaceholder("Cari nama paket layanan...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setValueChangeTimeout(300);
        searchField.addValueChangeListener(e -> refreshGrid());
        searchField.setWidth("340px");

        Button addBtn = new Button("Tambah Layanan", VaadinIcon.PLUS.create(), e -> openServiceDialog(null));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addBtn.getStyle().set("background", "var(--primary-gradient)")
                .set("box-shadow", "0 4px 12px rgba(79, 70, 229, 0.25)")
                .set("border-radius", "10px");

        Div toolbar = new Div();
        toolbar.addClassName("responsive-toolbar");
        toolbar.add(searchField, addBtn);

        add(title, subtitle, toolbar);
    }

    private void createGridAndCards() {
        grid.setSizeFull();
        grid.addClassName("desktop-only");

        grid.addColumn(LaundryService::getName).setHeader("Nama Layanan").setSortable(true).setFlexGrow(2);
        grid.addColumn(s -> ReceiptService.formatRupiah(s.getPrice()) + " / " + s.getUnit().name().toLowerCase())
                .setHeader("Tarif").setAutoWidth(true);
        grid.addColumn(s -> s.getEstimatedDurationHours() + " Jam").setHeader("Estimasi Durasi").setAutoWidth(true);

        grid.addComponentColumn(service -> {
            Span badge = new Span(service.isActive() ? "Aktif" : "Nonaktif");
            badge.addClassName("status-badge");
            badge.addClassName(service.isActive() ? "badge-selesai" : "badge-belum-lunas");
            return badge;
        }).setHeader("Status").setAutoWidth(true);

        grid.addComponentColumn(service -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            Button toggleBtn = new Button(service.isActive() ? "Nonaktifkan" : "Aktifkan", e -> {
                servicePackageService.toggleActive(service.getId());
                showNotification("Status layanan berhasil diubah", NotificationVariant.LUMO_SUCCESS);
                refreshGrid();
            });
            toggleBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openServiceDialog(service));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            editBtn.setTooltipText("Edit Layanan");

            Button delBtn = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(service));
            delBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            delBtn.setTooltipText("Hapus Layanan");

            actions.add(toggleBtn, editBtn, delBtn);
            return actions;
        }).setHeader("Aksi").setAutoWidth(true);

        Div cardWrapper = new Div();
        cardWrapper.addClassName("modern-card");
        cardWrapper.getStyle().set("padding", "0").set("overflow", "hidden");
        cardWrapper.add(grid);

        mobileCardContainer.addClassName("mobile-only");
        mobileCardContainer.setWidthFull();

        add(cardWrapper, mobileCardContainer);
    }

    private void refreshGrid() {
        List<LaundryService> services = servicePackageService.search(searchField.getValue());
        grid.setItems(services);
        renderMobileCards(services);
    }

    private void renderMobileCards(List<LaundryService> services) {
        mobileCardContainer.removeAll();

        if (services.isEmpty()) {
            Div emptyMsg = new Div();
            emptyMsg.setText("Tidak ada paket layanan yang ditemukan.");
            emptyMsg.getStyle().set("color", "#9C96A8").set("padding", "2rem").set("text-align", "center");
            mobileCardContainer.add(emptyMsg);
            return;
        }

        for (LaundryService service : services) {
            Div card = new Div();
            card.addClassName("mobile-order-card");

            Div row1 = new Div();
            row1.addClassName("mobile-order-card-header");
            Span name = new Span(service.getName());
            name.getStyle().set("font-weight", "800").set("font-size", "1rem");

            Span statusBadge = new Span(service.isActive() ? "Aktif" : "Nonaktif");
            statusBadge.addClassNames("status-badge", service.isActive() ? "badge-selesai" : "badge-belum-lunas");
            row1.add(name, statusBadge);

            Div row2 = new Div();
            row2.getStyle().set("display", "flex").set("justify-content", "space-between").set("align-items", "center");
            Span price = new Span(ReceiptService.formatRupiah(service.getPrice()) + " / " + service.getUnit().name().toLowerCase());
            price.getStyle().set("font-weight", "800").set("color", "var(--text-main)");

            Span dur = new Span("⏱ " + service.getEstimatedDurationHours() + " Jam");
            dur.getStyle().set("font-size", "0.85rem").set("color", "#6C667A");
            row2.add(price, dur);

            Div row3 = new Div();
            row3.getStyle().set("display", "flex").set("justify-content", "flex-end").set("gap", "8px").set("margin-top", "6px");

            Button toggleBtn = new Button(service.isActive() ? "Nonaktifkan" : "Aktifkan", e -> {
                servicePackageService.toggleActive(service.getId());
                showNotification("Status layanan berhasil diubah", NotificationVariant.LUMO_SUCCESS);
                refreshGrid();
            });
            toggleBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

            Button editBtn = new Button("Edit", VaadinIcon.EDIT.create(), e -> openServiceDialog(service));
            editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

            Button delBtn = new Button("Hapus", VaadinIcon.TRASH.create(), e -> confirmDelete(service));
            delBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

            row3.add(toggleBtn, editBtn, delBtn);

            card.add(row1, row2, row3);
            mobileCardContainer.add(card);
        }
    }

    private void openServiceDialog(LaundryService service) {
        boolean isEdit = service != null;
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isEdit ? "Edit Paket Layanan" : "Tambah Paket Layanan Baru");

        TextField nameField = new TextField("Nama Layanan");
        nameField.setRequired(true);
        nameField.setPlaceholder("Contoh: Cuci Kering Reguler");
        nameField.setWidthFull();

        BigDecimalField priceField = new BigDecimalField("Harga Tarif (Rp)");
        priceField.setRequired(true);
        priceField.setPlaceholder("Contoh: 10000");
        priceField.setWidthFull();

        Select<ServiceUnit> unitSelect = new Select<>();
        unitSelect.setLabel("Satuan");
        unitSelect.setItems(ServiceUnit.values());
        unitSelect.setItemLabelGenerator(ServiceUnit::getDisplayName);
        unitSelect.setValue(ServiceUnit.KG);
        unitSelect.setWidthFull();

        IntegerField durationField = new IntegerField("Estimasi Durasi (Jam)");
        durationField.setRequired(true);
        durationField.setMin(1);
        durationField.setStep(1);
        durationField.setValue(24);
        durationField.setWidthFull();

        Checkbox activeCheckbox = new Checkbox("Layanan Aktif", true);

        if (isEdit) {
            nameField.setValue(service.getName());
            priceField.setValue(service.getPrice());
            unitSelect.setValue(service.getUnit());
            durationField.setValue(service.getEstimatedDurationHours());
            activeCheckbox.setValue(service.isActive());
        }

        VerticalLayout form = new VerticalLayout(nameField, priceField, unitSelect, durationField, activeCheckbox);
        form.setPadding(false);
        form.setSpacing(true);
        dialog.add(form);

        Button saveBtn = new Button(isEdit ? "Simpan Perubahan" : "Simpan Layanan", e -> {
            try {
                if (isEdit) {
                    servicePackageService.updateService(service.getId(), nameField.getValue(),
                            priceField.getValue(), unitSelect.getValue(), durationField.getValue(), activeCheckbox.getValue());
                    showNotification("Paket layanan berhasil diperbarui", NotificationVariant.LUMO_SUCCESS);
                } else {
                    servicePackageService.createService(nameField.getValue(),
                            priceField.getValue(), unitSelect.getValue(), durationField.getValue(), activeCheckbox.getValue());
                    showNotification("Paket layanan baru berhasil disimpan", NotificationVariant.LUMO_SUCCESS);
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

    private void confirmDelete(LaundryService service) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Hapus Layanan");

        confirmDialog.add(new Paragraph("Apakah Anda yakin ingin menghapus layanan '" + service.getName() + "'?"));

        Button deleteBtn = new Button("Hapus", e -> {
            try {
                servicePackageService.deleteService(service.getId());
                showNotification("Layanan berhasil dihapus", NotificationVariant.LUMO_SUCCESS);
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
