# UC-008: View Financial Dashboard

> Copy this template for each feature as `use-case-NNN-short-name.md`.
> Replace all `[bracketed text]` with your content. Remove sections that genuinely do not apply (e.g. no secondary actor), but do not invent shortcuts — Preconditions, Trigger, Main Flow, and Postconditions are mandatory.

---

**Goal:** As a store owner, I want to view a financial dashboard showing revenue, expenses, and profitability metrics so that I can monitor business performance and make informed decisions.

**Status:** Pending
**Date:** 2024-01-01

> A use case cannot be marked as **Implemented** unless all criteria in the use-case implementation workflow are fulfilled.

---

## Actors

- **Primary actor:** Store Owner

---

## Preconditions

- Store owner is authenticated and logged into the system with OWNER role
- At least one order and/or expense exists in the system

---

## Trigger

Store owner clicks "Dashboard", "Financial Summary", or "Reports" in the owner menu.

---

## Main Flow

1. System displays the Financial Dashboard.
2. System calculates financial metrics for a default date range (e.g., current month or last 30 days).
3. System displays key performance indicators (KPIs) in prominent cards/widgets:
   - **Total Revenue:** Sum of all order totals for the period
   - **Total Expenses:** Sum of all logged expenses for the period
   - **Net Profit:** Total revenue - total expenses
   - **Order Count:** Number of orders completed in the period
   - **Average Order Value:** Total revenue / order count
4. System displays a visual summary (chart or graph) showing:
   - Revenue trend over time (daily or weekly)
   - Expense trend over time
   - Revenue vs. Expenses comparison
5. System displays a breakdown by service category (optional):
   - Revenue by service type (weight-based vs. item-based)
   - Revenue by service (top services by revenue)
6. Store owner can filter the dashboard by date range using a date range picker.
7. Store owner selects a start date and end date.
8. System updates all metrics and charts based on the selected date range.
9. System displays the filtered results with the selected date range displayed prominently.
10. Store owner can optionally export or print the dashboard.

---

## Alternative Flows

### AF-1: Custom Date Range Selection

**Branches from:** Main Flow step 6
**Condition:** Store owner selects a custom date range instead of default

1. Store owner clicks on the date range filter.
2. System displays a date range picker.
3. Store owner selects a start date and end date.
4. Store owner clicks "Apply" or the selection auto-applies.
5. System recalculates all metrics for the selected date range.
6. Returns to Main Flow step 9.

### AF-2: View Category Breakdown

**Branches from:** Main Flow step 5
**Condition:** Store owner clicks on a service category to drill down

1. Store owner clicks on a category in the breakdown section (e.g., "Weight-Based Services").
2. System displays a detailed list of services within that category showing individual revenue.
3. Store owner can sort by revenue, order count, or service name.
4. Use case ends or returns to Main Flow step 6.

### AF-3: Export Dashboard Data

**Branches from:** Main Flow step 10
**Condition:** Store owner clicks "Export" or "Download"

1. Store owner clicks "Export as CSV" or "Export as PDF".
2. System generates a file with dashboard data (KPIs, date range, metrics).
3. System downloads or displays the file for the store owner.
4. Use case ends.

### AF-4: Print Dashboard

**Branches from:** Main Flow step 10
**Condition:** Store owner clicks "Print Dashboard"

1. Store owner clicks "Print" or "Print Dashboard".
2. System displays a print-friendly version of the dashboard.
3. Store owner confirms printing to send to local printer.
4. System sends the dashboard to the configured printer.
5. Use case ends.

### AF-5: No Data Available

**Branches from:** Main Flow step 2
**Condition:** No orders or expenses exist for the selected date range

1. System displays a message: "No data available for the selected date range."
2. System suggests expanding the date range or provides a link to create orders/expenses.
3. Store owner can adjust the date range or navigate away.
4. Use case ends.

---

## Postconditions

- **On success:** Dashboard displays accurate financial metrics and visualizations for the selected date range. Store owner can review performance and make business decisions.
- **On failure:** Dashboard cannot load; system displays an error message. Store owner can retry or navigate away.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Total Revenue is calculated as sum of all order totals (including prepaid and due amounts) for the date range |
| BR-02 | Total Expenses is sum of all logged expenses for the date range |
| BR-03 | Net Profit = Total Revenue - Total Expenses |
| BR-04 | Average Order Value = Total Revenue / Order Count (or 0 if no orders) |
| BR-05 | Date range filter applies to all metrics and visualizations simultaneously |
| BR-06 | Metrics are calculated in real-time based on the latest order and expense data |
| BR-07 | Only store owners can view the financial dashboard |
| BR-08 | Historical data is preserved for auditing and trend analysis |

---

## Tests

> Tests verify the flows and business rules above. There is no separate acceptance-criteria list — the flows and rules *are* the acceptance criteria.

- [ ] Main Flow covered (steps 1–10)
- [ ] AF-1, AF-2, AF-3, AF-4, AF-5 covered
- [ ] BR-01 through BR-08 covered

---

## UI Surface

> What the user sees and where they reach it. Keep this implementation-agnostic — no framework annotations, component class names, or file paths.

- **Financial Dashboard Layout:**
  - Header with title "Financial Summary" and date range filter
  - KPI cards displaying Total Revenue, Total Expenses, Net Profit, Order Count, Average Order Value
  - Charts/graphs for revenue trend, expense trend, and comparison
  - Service breakdown section with revenue by service type/category
  - "Export" and "Print" buttons in top-right corner
- **Date Range Filter:** Calendar date pickers for start and end date, "Apply" button.
- **Category Breakdown:** Expandable section showing revenue by service category with sorting options.
- **Export/Print Dialog:** Format selection (CSV, PDF), confirmation buttons.

| Page | Access |
|------|--------|
| Financial Dashboard | Store Owner |
| Date Range Filter | Store Owner |
| Category Breakdown | Store Owner |
| Export/Print Dialog | Store Owner |
