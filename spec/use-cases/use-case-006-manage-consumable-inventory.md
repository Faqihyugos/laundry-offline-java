# UC-006: Monitor and Manage Consumable Inventory

> Copy this template for each feature as `use-case-NNN-short-name.md`.
> Replace all `[bracketed text]` with your content. Remove sections that genuinely do not apply (e.g. no secondary actor), but do not invent shortcuts — Preconditions, Trigger, Main Flow, and Postconditions are mandatory.

---

**Goal:** As a store owner, I want to monitor consumable inventory levels (e.g., detergent, fabric softener) and receive low-stock warnings so that I can reorder supplies before running out.

**Status:** Pending
**Date:** 2024-01-01

> A use case cannot be marked as **Implemented** unless all criteria in the use-case implementation workflow are fulfilled.

---

## Actors

- **Primary actor:** Store Owner

---

## Preconditions

- Store owner is authenticated and logged into the system with OWNER role
- At least one consumable item exists in the inventory

---

## Trigger

Store owner clicks "Inventory" or "Consumables" in the owner dashboard or settings menu, or the system displays a low-stock warning on the owner dashboard.

---

## Main Flow

1. System displays the Consumable Inventory management screen.
2. System shows a list of consumable items with columns: name, current quantity, unit, low-stock threshold, last restocked date, status (OK, LOW, CRITICAL).
3. Store owner can filter or search consumables by name.
4. For each consumable, the system displays a status indicator:
   - GREEN (OK): Current quantity ≥ low-stock threshold
   - YELLOW (LOW): Current quantity < low-stock threshold but > 0
   - RED (CRITICAL): Current quantity = 0
5. Store owner clicks on a consumable to view details.
6. System displays consumable details: name, unit, current quantity, low-stock threshold, usage trend (optional), and an "Update Quantity" button.
7. Store owner clicks "Update Quantity" or "Restock".
8. System displays a form with current quantity and input field for new quantity.
9. Store owner enters the quantity after restocking (e.g., adds 10 kg of detergent).
10. Store owner clicks "Save" or "Confirm".
11. System updates the consumable quantity and records the last restocked date as today.
12. System displays a confirmation message.

---

## Alternative Flows

### AF-1: Add New Consumable

**Branches from:** Main Flow step 1
**Condition:** Store owner wants to add a new consumable item to track

1. Store owner clicks "Add Consumable" on the inventory screen.
2. System displays a form with fields: name, unit (kg, liters, bottles, etc.), initial quantity, low-stock threshold.
3. Store owner enters the consumable name (e.g., "Laundry Detergent").
4. Store owner selects the unit (dropdown).
5. Store owner enters the initial/current quantity.
6. Store owner enters the low-stock threshold (quantity at which to trigger warning).
7. Store owner clicks "Save".
8. System validates the input.
9. System creates the consumable record.
10. Use case ends.

### AF-2: Delete Consumable

**Branches from:** Main Flow step 1
**Condition:** Store owner wants to remove a consumable from tracking

1. Store owner clicks "Delete" on a consumable in the list.
2. System displays a confirmation dialog.
3. Store owner confirms.
4. System removes the consumable from the inventory.
5. Use case ends.

### AF-3: Low-Stock Warning on Dashboard

**Branches from:** Trigger
**Condition:** System displays a low-stock warning alert on the owner dashboard

1. System highlights consumables with YELLOW (LOW) or RED (CRITICAL) status.
2. Store owner clicks on a warning to navigate to the consumable details.
3. System displays the consumable and allows immediate restocking via AF-1 flow or Main Flow steps 7–12.
4. Use case ends.

### AF-4: Missing Required Fields

**Branches from:** AF-1 step 8
**Condition:** Name or low-stock threshold is empty

1. System displays an error message.
2. Store owner fills in missing fields.
3. Returns to AF-1 step 8.

---

## Postconditions

- **On success:** Consumable inventory is updated. Last restocked date is recorded. Status indicators reflect the new quantities. Warnings are displayed if quantities fall below threshold.
- **On failure:** Consumable quantities remain unchanged. Store owner sees an error message and can retry.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Consumable name is mandatory and must be unique |
| BR-02 | Unit is mandatory (kg, liters, bottles, pcs, etc.) |
| BR-03 | Current quantity must be a non-negative number |
| BR-04 | Low-stock threshold is mandatory and must be a positive number |
| BR-05 | Status is GREEN if quantity ≥ threshold, YELLOW if 0 < quantity < threshold, RED if quantity = 0 |
| BR-06 | Last restocked date is auto-recorded when quantity is updated |
| BR-07 | Only store owners can add, edit, or delete consumables |
| BR-08 | Low-stock warnings are displayed prominently on the owner dashboard |

---

## Tests

> Tests verify the flows and business rules above. There is no separate acceptance-criteria list — the flows and rules *are* the acceptance criteria.

- [ ] Main Flow covered (steps 1–12)
- [ ] AF-1, AF-2, AF-3, AF-4 covered
- [ ] BR-01 through BR-08 covered

---

## UI Surface

> What the user sees and where they reach it. Keep this implementation-agnostic — no framework annotations, component class names, or file paths.

- **Inventory List:** Table with consumable name, quantity, unit, threshold, status indicator (color-coded), last restocked date; "Add", "Edit", "Delete" buttons; search/filter bar.
- **Consumable Details:** Name, unit, current quantity, threshold, last restocked date, status badge, "Update Quantity" button, delete option.
- **Update Quantity Form:** Current quantity (read-only or editable), new quantity input, "Save" and "Cancel" buttons.
- **Add Consumable Form:** Text inputs for name, dropdown for unit, number inputs for initial quantity and low-stock threshold.
- **Low-Stock Warning Widget:** Dashboard component showing count of low/critical items with link to inventory details.

| Page | Access |
|------|--------|
| Consumable Inventory | Store Owner |
| Consumable Details | Store Owner |
| Update Quantity Form | Store Owner |
| Add Consumable Form | Store Owner |
| Owner Dashboard (with warnings) | Store Owner |
