# UC-009: View Order Summary Dashboard

> Copy this template for each feature as `use-case-NNN-short-name.md`.
> Replace all `[bracketed text]` with your content. Remove sections that genuinely do not apply (e.g. no secondary actor), but do not invent shortcuts — Preconditions, Trigger, Main Flow, and Postconditions are mandatory.

---

**Goal:** As a store owner, I want to view an order summary dashboard showing order counts, status distribution, and customer insights so that I can understand operational workload and customer behavior.

**Status:** Pending
**Date:** 2024-01-01

> A use case cannot be marked as **Implemented** unless all criteria in the use-case implementation workflow are fulfilled.

---

## Actors

- **Primary actor:** Store Owner

---

## Preconditions

- Store owner is authenticated and logged into the system with OWNER role
- At least one order exists in the system

---

## Trigger

Store owner clicks "Order Dashboard", "Order Summary", or "Operations" in the owner menu.

---

## Main Flow

1. System displays the Order Summary Dashboard.
2. System calculates order metrics for a default date range (e.g., current day or current week).
3. System displays key operational indicators (KPIs) in prominent cards/widgets:
   - **Total Orders:** Number of orders created in the period
   - **Orders by Status:** Breakdown showing count in each status (INTAKE, WASHING, DRYING, FOLDING, READY, PICKED_UP)
   - **Completed Orders:** Orders with status PICKED_UP
   - **Pending Orders:** Orders not yet picked up (statuses INTAKE through READY)
   - **Average Processing Time:** Average time from INTAKE to PICKED_UP (optional)
4. System displays a visual summary (chart or graph) showing:
   - Distribution of orders by status (pie chart or bar chart)
   - Order volume trend over time (daily or weekly)
   - Service utilization (which services are most used)
5. System displays customer insights:
   - **New Customers:** Count of customers registered in the period
   - **Repeat Customers:** Count of customers with 2+ orders
   - **Top Customers:** List of customers by order count or revenue
6. Store owner can filter the dashboard by date range using a date range picker.
7. Store owner selects a start date and end date.
8. System updates all metrics and charts based on the selected date range.
9. System displays the filtered results with the selected date range displayed prominently.
10. Store owner can optionally drill down on a status to see the list of orders in that status.

---

## Alternative Flows

### AF-1: Custom Date Range Selection

**Branches from:** Main Flow step 6
**Condition:** Store owner selects a custom date range

1. Store owner clicks on the date range filter.
2. System displays a date range picker.
3. Store owner selects a start date and end date.
4. Store owner clicks "Apply" or the selection auto-applies.
5. System recalculates all metrics for the selected date range.
6. Returns to Main Flow step 9.

### AF-2: View Orders by Status

**Branches from:** Main Flow step 10
**Condition:** Store owner clicks on a status to view detailed order list

1. Store owner clicks on a status in the status breakdown (e.g., "READY").
2. System displays a filtered list of all orders with that status, showing: order ID, customer name, date created, service items count.
3. Store owner can click on an order to view full details or update status.
4. Use case ends or returns to Main Flow.

### AF-3: View Top Customers

**Branches from:** Main Flow step 5
**Condition:** Store owner clicks on "Top Customers" section to drill down

1. Store owner clicks "View All" or on the top customers list.
2. System displays a detailed list of customers sorted by order count or total spent (configurable).
3. Store owner can click on a customer to view their order history and details.
4. Use case ends or returns to Main Flow.

### AF-4: Export Order Summary

**Branches from:** Main Flow step 10
**Condition:** Store owner clicks "Export" or "Download"

1. Store owner clicks "Export as CSV" or "Export as PDF".
2. System generates a file with order summary data (KPIs, metrics, customer insights).
3. System downloads or displays the file.
4. Use case ends.

### AF-5: Print Order Summary

**Branches from:** Main Flow step 10
**Condition:** Store owner clicks "Print Dashboard"

1. Store owner clicks "Print" or "Print Dashboard".
2. System displays a print-friendly version of the order summary.
3. Store owner confirms printing.
4. System sends the dashboard to the configured printer.
5. Use case ends.

### AF-6: No Data Available

**Branches from:** Main Flow step 2
**Condition:** No orders exist for the selected date range

1. System displays a message: "No orders found for the selected date range."
2. System suggests expanding the date range or provides a link to create orders.
3. Store owner can adjust the date range or navigate away.
4. Use case ends.

---

## Postconditions

- **On success:** Dashboard displays accurate order metrics and visualizations for the selected date range. Store owner can understand operational workload and customer behavior.
- **On failure:** Dashboard cannot load; system displays an error message. Store owner can retry or navigate away.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Total Orders = count of all orders created in the date range |
| BR-02 | Pending Orders = count of orders with status not equal to PICKED_UP |
| BR-03 | Completed Orders = count of orders with status PICKED_UP |
| BR-04 | Average Processing Time = average of (PICKED_UP timestamp - INTAKE timestamp) for completed orders |
| BR-05 | New Customers = count of unique customers with created_date within the date range |
| BR-06 | Repeat Customers = count of customers with 2 or more orders in the system (not limited to date range) |
| BR-07 | Service utilization shows count of times each service appears in orders within the date range |
| BR-08 | Date range filter applies to all metrics and visualizations simultaneously |
| BR-09 | Only store owners can view the order summary dashboard |
| BR-10 | Metrics are calculated in real-time based on the latest order data |

---

## Tests

> Tests verify the flows and business rules above. There is no separate acceptance-criteria list — the flows and rules *are* the acceptance criteria.

- [ ] Main Flow covered (steps 1–10)
- [ ] AF-1, AF-2, AF-3, AF-4, AF-5, AF-6 covered
- [ ] BR-01 through BR-10 covered

---

## UI Surface

> What the user sees and where they reach it. Keep this implementation-agnostic — no framework annotations, component class names, or file paths.

- **Order Summary Dashboard Layout:**
  - Header with title "Order Summary" and date range filter
  - KPI cards displaying Total Orders, Orders by Status breakdown, Completed Orders, Pending Orders, Average Processing Time
  - Charts/graphs for status distribution (pie/bar), order volume trend, service utilization
  - Customer insights section showing New Customers, Repeat Customers, Top Customers list
  - "Export" and "Print" buttons in top-right corner
- **Date Range Filter:** Calendar date pickers for start and end date, "Apply" button.
- **Status Breakdown:** Clickable status cards/sections to drill down and view order list.
- **Top Customers Section:** List showing customer name, order count, total spent (optional); "View All" link.
- **Export/Print Dialog:** Format selection, confirmation buttons.

| Page | Access |
|------|--------|
| Order Summary Dashboard | Store Owner |
| Date Range Filter | Store Owner |
| Filtered Order List (by Status) | Store Owner |
| Customer Details | Store Owner |
| Export/Print Dialog | Store Owner |
