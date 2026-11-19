# UC-002: Create Order

> Copy this template for each feature as `use-case-NNN-short-name.md`.
> Replace all `[bracketed text]` with your content. Remove sections that genuinely do not apply (e.g. no secondary actor), but do not invent shortcuts — Preconditions, Trigger, Main Flow, and Postconditions are mandatory.

---

**Goal:** As a cashier, I want to create a new order for a customer combining weight-based and item-based services with optional rush/express pricing so that I can process laundry intake and generate receipts.

**Status:** Pending
**Date:** 2024-01-01

> A use case cannot be marked as **Implemented** unless all criteria in the use-case implementation workflow are fulfilled.

---

## Actors

- **Primary actor:** Cashier

---

## Preconditions

- Cashier is authenticated and logged into the system with CASHIER role
- At least one customer exists in the system
- At least one service is configured in the service catalog

---

## Trigger

Cashier clicks "Create New Order" or selects an existing customer to create an order for them.

---

## Main Flow

1. System displays order creation screen with a customer search/selection field.
2. Cashier searches for or selects an existing customer.
3. System displays the selected customer's details (name, phone, prepaid balance).
4. System displays the available service catalog (weight-based and item-based services).
5. Cashier selects a service and enters the quantity (kg for weight-based, pcs for item-based).
6. System calculates the item subtotal based on quantity and base price.
7. Cashier can optionally mark the service item as "Rush/Express" for expedited processing.
8. System applies rush multiplier (1.5x) to the item price if rush is selected.
9. Cashier adds the item to the order (repeats steps 5–8 for multiple items).
10. Cashier reviews the order summary: list of items, unit prices, quantities, subtotals, and total amount.
11. Cashier can enter optional notes (e.g., "Handle with care").
12. Cashier clicks "Confirm Order".
13. System creates the order with status INTAKE.
14. System displays order confirmation with order ID and total amount.

---

## Alternative Flows

### AF-1: Apply Prepaid Balance

**Branches from:** Main Flow step 10
**Condition:** Customer has available prepaid balance and cashier chooses to apply it

1. Cashier selects "Apply Prepaid Balance" or views the prepaid amount.
2. System shows available prepaid balance and calculates the remaining amount due.
3. System applies the prepaid balance to reduce the total amount due.
4. Cashier reviews updated totals (original total, prepaid applied, final amount due).
5. Returns to Main Flow step 12.

### AF-2: Remove Service Item

**Branches from:** Main Flow step 9
**Condition:** Cashier realizes a service item was added in error

1. Cashier clicks "Remove" on the service item in the order preview.
2. System removes the item from the order and recalculates totals.
3. Returns to Main Flow step 9 (cashier can add another item or proceed).

### AF-3: Clear Entire Order

**Branches from:** Main Flow step 10
**Condition:** Cashier decides to cancel the order creation

1. Cashier clicks "Cancel Order".
2. System displays a confirmation prompt.
3. Cashier confirms cancellation.
4. Use case ends without creating an order.

### AF-4: Customer Has No Balance Due

**Branches from:** Main Flow step 10
**Condition:** Customer's prepaid balance fully covers the order total

1. System applies the prepaid balance.
2. System shows "Balance Due: 0.00" and indicates order is fully prepaid.
3. Cashier proceeds to Main Flow step 12.
4. Order is created with prepaid_applied equal to the total amount.

---

## Postconditions

- **On success:** A new order is created with status INTAKE, associated with the selected customer and cashier. Order items are persisted with correct pricing (including rush multiplier if applied). Prepaid balance is applied if chosen. Order is ready for status updates and printing.
- **On failure:** No order is created. Cashier returns to the order creation screen to retry or select a different customer.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | An order must have at least one service item |
| BR-02 | Quantity must be a positive number (> 0) |
| BR-03 | Weight-based services use kg as the unit; item-based services use pcs as the unit |
| BR-04 | Rush/Express multiplier is 1.5x the base price for a service item |
| BR-05 | Prepaid balance cannot exceed the order total and is applied in full or not at all |
| BR-06 | Order total is the sum of all item subtotals (base price × quantity × rush multiplier if applicable) |
| BR-07 | Customer prepaid balance is deducted only upon order confirmation, not during preview |

---

## Tests

> Tests verify the flows and business rules above. There is no separate acceptance-criteria list — the flows and rules *are* the acceptance criteria.

- [ ] Main Flow covered (steps 1–14)
- [ ] AF-1, AF-2, AF-3, AF-4 covered
- [ ] BR-01 through BR-07 covered

---

## UI Surface

> What the user sees and where they reach it. Keep this implementation-agnostic — no framework annotations, component class names, or file paths.

- **Order Creation Screen:** Customer selection dropdown, service catalog list, quantity input fields, rush/express checkbox per item, "Add Item" button.
- **Order Summary:** Table showing items (service name, quantity, unit price, rush indicator, subtotal), customer prepaid balance display, optional prepaid application button, notes field.
- **Order Confirmation:** Order ID, timestamp, customer info, itemized receipt, total amount, payment status (prepaid/due).

| Page | Access |
|------|--------|
| Order Creation | Cashier |
| Order Summary | Cashier |
| Order Confirmation | Cashier |
