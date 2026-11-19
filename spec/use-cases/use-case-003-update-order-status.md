# UC-003: Track and Update Order Status

**Goal:** As a cashier or store owner, I want to view active laundry orders, track their progress, update their status across the 4 stages (Pending → Proses → Selesai → Diambil), and settle any outstanding payments upon pickup so that operational workflow is smooth and customer laundry is accounted for.

**Status:** Implemented
**Date:** 2026-11-19

> A use case cannot be marked as **Implemented** unless all criteria in the use-case implementation workflow are fulfilled.

---

## Actors

- **Primary actor:** Cashier
- **Secondary actor:** Store Owner

---

## Preconditions

- User is authenticated and logged into the system
- At least one laundry order exists in the system

---

## Trigger

User navigates to the Order List view or clicks on an order from the Dashboard.

---

## Main Flow

1. User opens the Order Management view.
2. System displays a list of orders with filter tabs: All, Pending, Proses, Selesai, Diambil; each row showing Invoice Number, Customer Name, Order Date, Estimated Completion Date, Item Count, Total Amount, Payment Status badge (`Lunas` / `Belum Lunas` / `Sebagian`), and Order Status badge (`Pending` / `Proses` / `Selesai` / `Diambil`).
3. User selects or clicks on an order to open the Order Details view.
4. System displays full order details: Customer contact info, line items list, payment history, and current status in the workflow:
   `Pending` → `Proses` → `Selesai` → `Diambil`
5. System displays the next available forward action button (e.g. if currently `Pending`, button displays "Mulai Proses"; if `Proses`, button displays "Selesaikan Cucian"; if `Selesai`, button displays "Tandai Diambil").
6. User clicks the action button to transition the order to the next stage.
7. System updates the order status, records the status update timestamp, and displays a success notification.
8. System refreshes the order view and status badge.

---

## Alternative Flows

### AF-1: Filter and Search Orders
**Branches from:** Main Flow step 2
**Condition:** User wants to locate orders by invoice number, customer name, phone, or specific status
1. User types in search box or clicks a status filter tab (`Pending`, `Proses`, `Selesai`, `Diambil`).
2. System immediately filters the order list in real-time.
3. User proceeds to Main Flow step 3.

### AF-2: Settle Remaining Payment on Pickup (Pelunasan Pembayaran)
**Branches from:** Main Flow step 5
**Condition:** Order status is `Selesai` transitioning to `Diambil`, but Payment Status is still `Belum Lunas` or `Sebagian`
1. User clicks "Tandai Diambil".
2. System detects that there is an outstanding balance (`remaining_amount > 0`) and displays a Payment Settlement dialog.
3. Dialog shows Total Amount, Amount Already Paid, and Remaining Balance Due.
4. User selects payment method (`Cash` / `Transfer`) and enters payment amount.
5. User confirms payment settlement.
6. System records the new payment, updates Payment Status to `Lunas` (`remaining_amount = 0`), and advances Order Status to `Diambil`.
7. System updates order and offers to print / WhatsApp receipt update.

### AF-3: Edit Order Details (When in Pending Status)
**Branches from:** Main Flow step 4
**Condition:** Customer wants to add/remove items or change notes while order is still `Pending`
1. User clicks "Edit Order".
2. System allows modifying line items, quantities, and notes if order is in `Pending` state.
3. User saves changes; system recalculates totals and updates the order.

### AF-4: Mark as Diambil Directly (Pickup Confirmation)
**Branches from:** Main Flow step 5
**Condition:** Order is already `Lunas` and ready for pickup (`Selesai`)
1. User clicks "Tandai Diambil".
2. System prompts for quick confirmation: "Pastikan pelanggan sudah memeriksa dan menerima cucian."
3. User confirms.
4. System updates status to `Diambil` with completion timestamp.

---

## Postconditions

- **On success:** Order status is updated to the next workflow stage with timestamp recorded. Outstanding payments are collected and recorded if settled during pickup.
- **On failure:** Order status remains unchanged; cashier is alerted if an invalid transition or payment settlement error occurred.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Standard workflow progression must follow: `Pending` → `Proses` → `Selesai` → `Diambil`. |
| BR-02 | Backward status transitions are restricted to prevent accidental workflow regressions. |
| BR-03 | Orders marked as `Diambil` are considered completed and finalized. |
| BR-04 | When an order is marked as `Diambil`, the system must warn or require settlement if payment is still `Belum Lunas` or `Sebagian`. |
| BR-05 | Orders in `Proses`, `Selesai`, or `Diambil` cannot have line items deleted or altered without administrator authorization. |
| BR-06 | Every status transition records the updating user (cashier/owner) and timestamp. |

---

## Tests

- [x] Main Flow covered (steps 1–8)
- [x] AF-1: Filter by status and search covered
- [x] AF-2: Settle remaining payment on pickup covered
- [x] AF-3: Edit order in Pending state covered
- [x] AF-4: Direct pickup confirmation when Lunas covered
- [x] BR-01 through BR-06 covered

---

## UI Surface

- **Order List View:**
  - Status filter tabs: All, Pending, Proses, Selesai, Diambil.
  - Search bar (Invoice number, Customer name, Phone).
  - Grid table: Invoice #, Customer, Date, Estimated Finish, Total, Payment Status Badge, Order Status Badge, Actions.
- **Order Details View:**
  - Header: Invoice Number, Status Badge, Payment Badge, Action buttons (Next Status, Print Struk, Share WA).
  - Customer Card: Name, Phone, Address, WhatsApp link.
  - Itemized List Table: Service name, Unit, Quantity, Price, Subtotal.
  - Payment Summary: Total Amount, Paid Amount, Remaining Balance, Payment History.
  - Workflow Stepper: Visual representation of `Pending` → `Proses` → `Selesai` → `Diambil`.
- **Payment Settlement Modal:** Total, Paid, Balance, Payment Method dropdown, Amount input, Confirm button.

| Page / Component | Access |
|------------------|--------|
| Order List | Cashier, Owner |
| Order Details View | Cashier, Owner |
| Payment Settlement Dialog | Cashier, Owner |
