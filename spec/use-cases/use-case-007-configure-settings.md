# UC-007: Configure Laundry and Printer Settings

**Goal:** As a store owner, I want to configure laundry business information (shop name, address, telephone), invoice prefix, and thermal printer settings so that receipts, invoices, and system defaults reflect our store's identity.

**Status:** Implemented
**Date:** 2026-11-19

> A use case cannot be marked as **Implemented** unless all criteria in the use-case implementation workflow are fulfilled.

---

## Actors

- **Primary actor:** Store Owner

---

## Preconditions

- Store Owner is authenticated and logged into the system with OWNER role

---

## Trigger

Store Owner clicks "Pengaturan" (Settings) in the navigation menu.

---

## Main Flow

1. Owner navigates to the Settings view.
2. System displays settings categorized into sections:
   - **Informasi Laundry:** Nama Laundry, Alamat Lengkap, Nomor Telepon / WhatsApp.
   - **Pengaturan Invoice:** Prefix Nomor Invoice (e.g., `LBW`, `INV`).
   - **Pengaturan Printer:** Ukuran Kertas Struk (`58mm` / `80mm`), Nama Printer / Tipe Koneksi.
3. System loads current saved settings from `AppSetting` (or displays default values if freshly installed).
4. Owner updates the business information:
   - Changes Nama Laundry (e.g., "Laundry Bersih Wangi").
   - Changes Alamat (e.g., "Jl. Sudirman No. 123, Jakarta").
   - Changes Nomor Telepon (e.g., "081234567890").
5. Owner modifies the Invoice Prefix (e.g. from `LBW` to `LND`).
6. Owner selects the thermal printer paper format (e.g. `58mm` or `80mm`).
7. Owner clicks "Simpan Pengaturan" (Save Settings).
8. System validates that the required fields (Laundry Name, Phone, and Prefix) are provided.
9. System saves all settings in the local database and displays a success notification: "Pengaturan berhasil disimpan."
10. All subsequent receipts, WhatsApp messages, and new invoices immediately use the updated settings.

---

## Alternative Flows

### AF-1: Test Thermal Printer Configuration
**Branches from:** Main Flow step 6
**Condition:** Owner wants to test whether the thermal printer is formatting correctly
1. Owner clicks "Cetak Struk Uji Coba" (Print Test Receipt).
2. System generates a dummy test receipt containing sample shop name, date, invoice number, and test line items formatted to the selected paper width (58mm or 80mm).
3. System sends test output to the printer or triggers browser thermal print preview.
4. Owner inspects output to ensure alignment, spacing, and paper cut match the printer.

### AF-2: Invalid Invoice Prefix
**Branches from:** Main Flow step 8
**Condition:** Owner enters an empty prefix or prefix with invalid symbols/spaces
1. System validates that prefix consists only of 2–6 alphanumeric characters (uppercase).
2. System displays error message: "Prefix invoice harus berupa 2-6 karakter huruf/angka tanpa spasi (contoh: LBW, INV)."
3. Owner corrects prefix and resubmits.
4. Returns to Main Flow step 7.

### AF-3: Reset to Default Settings
**Branches from:** Main Flow step 2
**Condition:** Owner wants to restore original default template values
1. Owner clicks "Kembalikan ke Default" (Restore Defaults).
2. System prompts for confirmation.
3. Owner confirms; system reloads default values (Name: "Laundry Bersih Wangi", Prefix: "LBW", Paper: "58mm").
4. Owner clicks "Simpan Pengaturan" to apply.

---

## Postconditions

- **On success:** Shop info, invoice prefix, and printer configurations are saved and persistently used throughout the application.
- **On failure:** Settings remain unchanged; validation messages highlight fields requiring attention.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Only users with OWNER role can modify system settings. |
| BR-02 | Nama Laundry and Nomor Telepon are mandatory to ensure receipts always show valid contact details. |
| BR-03 | Invoice Prefix must be 2 to 6 uppercase alphanumeric characters without whitespace. |
| BR-04 | Changing the Invoice Prefix does not alter past historical invoice numbers; it only affects newly generated invoices. |
| BR-05 | Supported paper widths are standard 58mm and 80mm. |

---

## Tests

- [x] Main Flow covered (steps 1–10)
- [x] AF-1: Test print receipt covered
- [x] AF-2: Invalid prefix validation covered
- [x] AF-3: Reset to default settings covered
- [x] BR-01 through BR-05 covered

---

## UI Surface

- **Settings Page Layout:**
  - Card 1: **Profil Laundry** — Text fields for Nama Laundry, Alamat, Nomor Telepon/WhatsApp.
  - Card 2: **Format Invoice** — Text field for Prefix Invoice, sample preview (e.g. `LBW-20261119-0001`).
  - Card 3: **Konfigurasi Printer Struk** — Radio/Dropdown for Paper Width (`58mm` / `80mm`), "Cetak Struk Uji Coba" button.
  - Footer: "Simpan Pengaturan" primary button and "Kembalikan Default" secondary button.

| Page / Component | Access |
|------------------|--------|
| Settings Page | Store Owner |
| Test Print Action | Store Owner |
