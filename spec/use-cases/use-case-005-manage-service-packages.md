# UC-005: Manage Service Packages

**Goal:** As a store owner, I want to manage laundry service packages (CRUD services, prices, units in kg/pcs, turnaround duration estimates, and active status) so that cashiers have an accurate, up-to-date catalog when accepting orders.

**Status:** Implemented
**Date:** 2026-11-19

> A use case cannot be marked as **Implemented** unless all criteria in the use-case implementation workflow are fulfilled.

---

## Actors

- **Primary actor:** Store Owner
- **Secondary actor:** Cashier (can view service list, but cannot edit or delete)

---

## Preconditions

- Store Owner is authenticated and logged into the system with OWNER role

---

## Trigger

Store Owner navigates to "Paket Layanan" (Service Packages) in the navigation menu.

---

## Main Flow

1. Owner opens the Service Packages management screen.
2. System displays a table of all existing service packages showing: Service Name, Price, Unit (`kg` or `pcs`), Estimated Duration (hours/days), and Status (`Aktif` / `Nonaktif`).
3. Owner clicks "Tambah Layanan" (Add Service).
4. System displays a modal form with fields:
   - Nama Layanan (e.g., "Cuci Kering", "Cuci Setrika", "Setrika Saja", "Bedcover Besar", "Sepatu").
   - Harga (Price per unit in Rupiah).
   - Satuan (Dropdown / Radio: `kg` for weight-based, `pcs` for unit-based).
   - Estimasi Durasi Pengerjaan (in hours, e.g. 24 hours / 1 day, 48 hours, 6 hours for express).
   - Status (Checkbox/Toggle: Aktif / Nonaktif, defaults to Aktif).
5. Owner enters service name, price, selects unit (`kg` or `pcs`), specifies turnaround duration estimate, and confirms status.
6. Owner clicks "Simpan" (Save).
7. System validates that the service name is unique and price is a positive number.
8. System saves the service package and displays a success notification.
9. System refreshes the table showing the newly created service.

---

## Alternative Flows

### AF-1: Edit Existing Service Package
**Branches from:** Main Flow step 2
**Condition:** Owner wants to update pricing, duration, or name of an existing service
1. Owner clicks "Edit" on a service package row.
2. System displays the form populated with current values.
3. Owner edits price, name, duration, or active toggle.
4. Owner clicks "Simpan Perubahan" (Save Changes).
5. System validates and updates the service package.
6. System confirms update and refreshes the table. Note: existing past orders preserve historical price snapshots.

### AF-2: Toggle Service Active / Nonaktif
**Branches from:** Main Flow step 2
**Condition:** Owner wants to temporarily suspend a service without deleting it
1. Owner toggles the status switch on a service row (or inside the edit modal) to `Nonaktif`.
2. System updates the status.
3. Inactive services remain visible in the owner's management table (badged as `Nonaktif`), but are hidden from the cashier's active order intake selector.

### AF-3: Delete Service Package
**Branches from:** Main Flow step 2
**Condition:** Owner wants to delete a service that was created by mistake
1. Owner clicks "Hapus" (Delete) on a service row.
2. If the service is already referenced by historical orders, system prevents hard deletion and recommends setting status to `Nonaktif` instead.
3. If the service has no order references, system prompts for confirmation.
4. Owner confirms; system deletes the service and displays success notification.

### AF-4: Validation Failure (Invalid Price or Missing Name)
**Branches from:** Main Flow step 7
**Condition:** Name is blank or price is <= 0
1. System highlights invalid inputs with an error message: "Nama layanan wajib diisi dan harga harus lebih besar dari 0."
2. Owner corrects the input and resubmits.
3. Returns to Main Flow step 6.

---

## Postconditions

- **On success:** Service package is created, updated, or toggled. Changes take effect immediately on the cashier's order intake view.
- **On failure:** Service package is not modified. Form displays validation errors.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Service name is mandatory and must be unique among active services. |
| BR-02 | Price must be a positive integer/number (> 0). |
| BR-03 | Unit must be strictly either `kg` (kilogram) or `pcs` (pieces). |
| BR-04 | Turnaround duration estimate must be greater than 0 (stored in hours, e.g. 24h). |
| BR-05 | Inactive services (`is_active = false`) must not appear as options in the order creation screen. |
| BR-06 | Deleting a service package that is used in past orders is prohibited to protect audit and financial integrity. |
| BR-07 | Only users with OWNER role can add, edit, toggle, or delete service packages. |

---

## Tests

- [x] Main Flow covered (steps 1–9)
- [x] AF-1: Edit service package covered
- [x] AF-2: Toggle active/inactive status covered
- [x] AF-3: Delete service package with constraint check covered
- [x] AF-4: Validation failure covered
- [x] BR-01 through BR-07 covered

---

## UI Surface

- **Service Package List View:**
  - Header with "Tambah Layanan" button.
  - Filter by status (All, Aktif, Nonaktif) and search by service name.
  - Grid table: Nama Layanan, Harga / Satuan, Estimasi Durasi, Status badge (`Aktif` [Green] / `Nonaktif` [Gray]), Actions (Edit, Delete).
- **Service Package Form Dialog:**
  - Form fields: Nama Layanan, Harga (Rp), Satuan radio/dropdown (`kg` / `pcs`), Estimasi Durasi (Jam), Status aktif toggle.
  - Action buttons: "Simpan" and "Batal".

| Page / Component | Access |
|------------------|--------|
| Service Package Management | Store Owner |
| Service Package Form Dialog | Store Owner |
