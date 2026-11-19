# UC-008: View Dashboard

**Goal:** As a store owner or cashier, I want to view a real-time dashboard displaying today's revenue summary, order counts categorized by workflow status, weekly revenue trends, and recent orders so that I can quickly assess store performance and operational priorities at a glance.

**Status:** Implemented
**Date:** 2026-11-19

> A use case cannot be marked as **Implemented** unless all criteria in the use-case implementation workflow are fulfilled.

---

## Actors

- **Primary actor:** Store Owner
- **Secondary actor:** Cashier (operational views)

---

## Preconditions

- User is authenticated and logged into the system

---

## Trigger

User logs in or clicks "Dashboard" in the navigation sidebar.

---

## Main Flow

1. User opens the Dashboard view.
2. System calculates and displays real-time key metrics:
   - **Pendapatan Hari Ini (Today's Revenue):** Sum of revenues/payments received today (in IDR).
   - **Total Order Hari Ini:** Count of orders registered today.
3. System calculates and displays status distribution cards:
   - Jumlah Order **Pending** (Waiting to be processed)
   - Jumlah Order **Proses** (Currently being washed/ironed)
   - Jumlah Order **Selesai** (Ready for pickup)
   - Jumlah Order **Diambil** (Completed & collected)
4. System displays the **Grafik Pendapatan Mingguan (Weekly Revenue Trend Chart)**:
   - Bar/line chart illustrating daily revenue over the last 7 days (Monday through Sunday or trailing 7 days).
5. System displays the **Daftar Order Terbaru (Latest 5 Orders)** table:
   - Showing the 5 most recent orders with columns: No. Invoice, Nama Pelanggan, Tanggal, Total (Rp), Status Bayar (`Lunas`/`Belum Lunas`/`Sebagian`), Status Order badge (`Pending`/`Proses`/`Selesai`/`Diambil`).
6. User can click on any order in the "Latest 5 Orders" list to jump directly to its Order Details view.
7. User can click on any status card (e.g. `Selesai`) to open the Order List pre-filtered by that status.

---

## Alternative Flows

### AF-1: Cashier View Restrictions
**Branches from:** Main Flow step 2
**Condition:** Logged-in user has role CASHIER
1. Per role-based access rules (no detailed financial reports for Cashier), system displays operational statistics (Order count today, orders by status, and latest 5 orders).
2. Financial revenue figures and weekly revenue charts are either restricted or scoped to daily register balance according to store configuration.
3. Cashier focuses on pending/processing/ready queues.

### AF-2: Fresh Installation / No Orders Yet
**Branches from:** Main Flow step 2
**Condition:** Store has just been installed and has no orders recorded
1. System shows Rp 0 for Today's Revenue and 0 for all status counts.
2. Chart displays a helpful empty state ("Belum ada data transaksi minggu ini").
3. Recent orders table shows: "Belum ada pesanan laundry. Buat order pertama Anda sekarang!" with a direct link/button to "Buat Order Baru".

### AF-3: Quick Navigation from Status Cards
**Branches from:** Main Flow step 7
**Condition:** User clicks on the "Selesai" (Ready) status count card
1. System navigates to the Order List page with the `Selesai` tab active.
2. User sees orders waiting for customer pickup.

---

## Postconditions

- **On success:** Dashboard renders accurate today's revenue, order counts by status, weekly trend graph, and 5 latest orders.
- **On failure:** Error message is shown; user can refresh dashboard.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Today's revenue calculation encompasses all settled payments recorded between 00:00:00 and 23:59:59 of current date. |
| BR-02 | Status counts must represent active orders accurately categorized into `Pending`, `Proses`, `Selesai`, and `Diambil`. |
| BR-03 | Weekly trend chart must display each day of the last 7 days clearly labeled. |
| BR-04 | Latest orders list must show exactly the 5 most recently created orders ordered descending by creation timestamp. |
| BR-05 | Full revenue analytics on the dashboard are visible to OWNER role. |

---

## Tests

- [x] Main Flow covered (steps 1–7)
- [x] AF-1: Cashier role rendering covered
- [x] AF-2: Empty state handling covered
- [x] AF-3: Navigation to filtered order list covered
- [x] BR-01 through BR-05 covered

---

## UI Surface

- **Dashboard Layout:**
  - Top Metrics Row:
    - Card: "Pendapatan Hari Ini" (Large IDR figure, e.g. Rp 450.000)
    - 4 Status Badged Metric Cards:
      - Pending (Blue indicator, count)
      - Proses (Orange indicator, count)
      - Selesai (Green indicator, count)
      - Diambil (Purple indicator, count)
  - Middle Section: "Grafik Pendapatan Mingguan" (Interactive visual bar/line chart of 7-day revenue).
  - Bottom Section: "Daftar 5 Order Terbaru" table with columns: Invoice #, Pelanggan, Layanan, Total, Status Bayar, Status Cucian, Action.

| Page / Component | Access |
|------------------|--------|
| Dashboard View | Store Owner, Cashier |
| Order Detail Shortcut | Store Owner, Cashier |
