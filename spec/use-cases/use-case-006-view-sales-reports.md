# UC-006: View Sales Reports and Export to Excel

**Goal:** As a store owner, I want to analyze laundry sales performance across daily, weekly, monthly, or custom periods with visual trend charts and export reports to Excel (.xlsx) so that I can evaluate business growth and maintain financial records.

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

Store Owner clicks "Laporan" (Reports) or "Laporan Penjualan" from the navigation menu.

---

## Main Flow

1. Owner opens the Sales Report screen.
2. System loads reports with default period: current month.
3. System calculates and displays summary KPI cards:
   - **Total Pendapatan (Total Revenue):** Total revenue earned within the selected period (in IDR).
   - **Total Transaksi (Transaction Count):** Total number of orders placed in the period.
   - **Rata-rata Transaksi (Average Order Value):** Total revenue divided by transaction count.
4. System renders a visual sales trend chart showing revenue per day or per week across the selected timeframe.
5. System displays a detailed table of transactions for the period:
   - Invoice Number
   - Tanggal Order (Date)
   - Nama Pelanggan (Customer Name)
   - Rincian Layanan (Services and Quantities)
   - Status Order (`Pending`, `Proses`, `Selesai`, `Diambil`)
   - Status Pembayaran (`Lunas`, `Belum Lunas`, `Sebagian`)
   - Metode Bayar (`Cash`, `Transfer`)
   - Total Nominal (Rp)
6. Owner can filter by period using preset tabs:
   - `Hari Ini` (Today)
   - `Minggu Ini` (Current Week)
   - `Bulan Ini` (Current Month)
   - `Custom` (Custom Date Range with Start Date and End Date pickers)
7. Owner clicks "Export Excel" (`.xlsx`).
8. System generates a formatted Excel file (`.xlsx`) containing:
   - Header with Laundry Shop name, address, report title, and selected period.
   - KPI Summary block (Total Revenue, Total Orders, Average Value).
   - Full transaction data table with styled columns, currency formatting, and totals formula row.
9. System prompts the browser to download the file (e.g., `laporan_penjualan_[start_date]_[end_date].xlsx`).
10. Owner saves and opens the Excel file.

---

## Alternative Flows

### AF-1: Filter by Custom Date Range
**Branches from:** Main Flow step 6
**Condition:** Owner selects "Custom" period tab
1. System reveals Start Date and End Date pickers.
2. Owner selects start date and end date (e.g. 2026-10-01 to 2026-10-31).
3. Owner clicks "Terapkan Filter" (Apply Filter).
4. System recalculates KPIs, updates the chart, and reloads the transaction table for that specific range.
5. Owner resumes at Main Flow step 7.

### AF-2: Filter by Payment Status or Payment Method
**Branches from:** Main Flow step 6
**Condition:** Owner wants to see only Cash transactions, Transfer transactions, or unpaid orders
1. Owner selects secondary filters (e.g., Payment Method: `Cash` only; or Payment Status: `Belum Lunas`).
2. System filters table rows and recalculates revenue accordingly.

### AF-3: No Transactions in Selected Period
**Branches from:** Main Flow step 3
**Condition:** No orders were placed in the selected period
1. System displays zero values for KPIs (Total Pendapatan: Rp 0, Total Transaksi: 0).
2. Chart displays an empty state placeholder ("Belum ada data penjualan pada periode ini").
3. Table shows an empty state message.
4. "Export Excel" button is disabled or exports an empty template with headers.

---

## Postconditions

- **On success:** Owner views sales statistics, examines revenue trend charts, and downloads an Excel spreadsheet (`.xlsx`) of the selected reporting period.
- **On failure:** Error notification is shown if date range is invalid; no export occurs.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Only users with OWNER role can access sales reports and financial data. |
| BR-02 | Period presets must calculate dates dynamically: Hari Ini (00:00 to 23:59 today), Minggu Ini (Monday to Sunday of current week), Bulan Ini (first to last day of current month). |
| BR-03 | In Custom Date Range, End Date must be greater than or equal to Start Date. |
| BR-04 | Revenue calculations must be based on actual total order value or settled revenue within the filtered period. |
| BR-05 | Exported Excel file must strictly be in standard `.xlsx` format, featuring proper column widths, numeric cell types for amounts, and formatted dates. |

---

## Tests

- [x] Main Flow covered (steps 1–10)
- [x] AF-1: Custom date range filter covered
- [x] AF-2: Payment method/status sub-filtering covered
- [x] AF-3: Empty period handling covered
- [x] BR-01 through BR-05 covered

---

## UI Surface

- **Sales Report Page:**
  - Header: Period selector tabs (`Hari Ini`, `Minggu Ini`, `Bulan Ini`, `Custom`), date pickers (visible on Custom), "Export Excel" button with green sheet icon.
  - KPI Cards row: Total Pendapatan (Rp), Jumlah Transaksi, Rata-rata Nilai Order.
  - Sales Trend Chart: Visual bar or line chart showing revenue progression over time.
  - Transaction Table: Date, Invoice #, Customer, Services, Order Status badge, Payment Status badge, Method, Total Amount.

| Page / Component | Access |
|------------------|--------|
| Sales Reports View | Store Owner |
| Excel Export Action | Store Owner |
