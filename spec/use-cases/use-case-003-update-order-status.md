# UC-003: Update Order Status

> Copy this template for each feature as `use-case-NNN-short-name.md`.
> Replace all `[bracketed text]` with your content. Remove sections that genuinely do not apply (e.g. no secondary actor), but do not invent shortcuts — Preconditions, Trigger, Main Flow, and Postconditions are mandatory.

---

**Goal:** As a cashier, I want to update the status of a laundry order as it moves through the workflow stages so that customers and staff can track progress.

**Status:** Pending
**Date:** 2024-01-01

> A use case cannot be marked as **Implemented** unless all criteria in the use-case implementation workflow are fulfilled.

---

## Actors

- **Primary actor:** Cashier

---

## Preconditions

- Cashier is authenticated and logged into the system with CASHIER role
- At least one order exists in the system with status INTAKE or earlier in the workflow

---

## Trigger

Cashier opens the order list or order details view and clicks "Update Status" or selects the next workflow stage.

---

## Main Flow

1. System displays a list of active orders or a search/filter interface.
2. Cashier searches for or selects an order by order ID or customer name.
3. System displays the selected order's details: customer, items, current status, and timestamp.
4. System shows available status transition options based on the current status.
5. Cashier selects the next status (e.g., from INTAKE → WASHING, WASHING → DRYING, etc.).
6. System optionally displays a confirmation prompt with the new status and timestamp.
7. Cashier confirms the status change.
8. System updates the order status and records the timestamp of the change.
9. System displays a confirmation message.
10. Cashier returns to the order list or order details view.

---

## Alternative Flows

### AF-1: Invalid Status Transition

**Branches from:** Main Flow step 4
**Condition:** Cashier attempts an invalid status transition (e.g., FOLDING → INTAKE)

1. System displays an error message: "Invalid status transition. Orders can only move forward in the workflow."
2. System re-displays the available valid status options.
3. Cashier selects a valid status.
4. Returns to Main Flow step 6.

### AF-2: Order Already Picked Up

**Branches from:** Main Flow step 4
**Condition:** Order status is already PICKED_UP

1. System displays a message: "This order has already been picked up and cannot be modified."
2. Cashier can view order details but cannot change status.
3. Use case ends.

### AF-3: Search Order Not Found

**Branches from:** Main Flow step 2
**Condition:** No order matches the search criteria

1. System displays: "No orders found. Please check the order ID or customer name and try again."
2. Cashier retries the search.
3. Returns to Main Flow step 2.

---

## Postconditions

- **On success:** Order status is updated to the new stage. Status change is persisted with a timestamp. Order history is maintained for audit purposes.
- **On failure:** Order status remains unchanged. Cashier sees an error message and can retry.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Valid status sequence is: INTAKE → WASHING → DRYING → FOLDING → READY → PICKED_UP |
| BR-02 | Status transitions are unidirectional (forward only); no backwards transitions allowed |
| BR-03 | A PICKED_UP order cannot be modified |
| BR-04 | Each status change must be recorded with a timestamp for audit trail |
| BR-05 | Only cashiers can update order status |

---

## Tests

> Tests verify the flows and business rules above. There is no separate acceptance-criteria list — the flows and rules *are* the acceptance criteria.

- [ ] Main Flow covered (steps 1–10)
- [ ] AF-1, AF-2, AF-3 covered
- [ ] BR-01 through BR-05 covered

---

## UI Surface

> What the user sees and where they reach it. Keep this implementation-agnostic — no framework annotations, component class names, or file paths.

- **Order List/Search:** Search bar for order ID or customer name, filtered list of active orders showing ID, customer name, current status.
- **Order Details:** Customer info, service items, current status badge, available status transition buttons, timestamp of last status update.
- **Status Update Confirmation:** Current status, proposed new status, timestamp, "Confirm" and "Cancel" buttons.

| Page | Access |
|------|--------|
| Order Search/List | Cashier |
| Order Details | Cashier |
| Status Update Confirmation | Cashier |
