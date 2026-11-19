# UC-005: Configure Service Catalog

> Copy this template for each feature as `use-case-NNN-short-name.md`.
> Replace all `[bracketed text]` with your content. Remove sections that genuinely do not apply (e.g. no secondary actor), but do not invent shortcuts — Preconditions, Trigger, Main Flow, and Postconditions are mandatory.

---

**Goal:** As a store owner, I want to configure the service catalog by adding, editing, and removing services and setting their pricing so that cashiers have the correct offerings and prices when creating orders.

**Status:** Pending
**Date:** 2024-01-01

> A use case cannot be marked as **Implemented** unless all criteria in the use-case implementation workflow are fulfilled.

---

## Actors

- **Primary actor:** Store Owner

---

## Preconditions

- Store owner is authenticated and logged into the system with OWNER role

---

## Trigger

Store owner clicks "Service Catalog" or "Configure Services" in the owner dashboard or settings menu.

---

## Main Flow

1. System displays the Service Catalog management screen.
2. System shows a list of currently configured services with columns: name, type (weight-based or item-based), base price, and unit.
3. Store owner clicks "Add Service" to create a new service.
4. System displays a form with fields: service name, service type (dropdown: Weight-based or Item-based), base price, unit (kg for weight-based, pcs for item-based).
5. Store owner enters the service name (e.g., "Regular Wash", "Dry Cleaning").
6. Store owner selects the service type from a dropdown.
7. Store owner enters the base price.
8. System auto-fills the unit based on service type selected.
9. Store owner clicks "Save Service".
10. System validates the input.
11. System creates the service and displays it in the catalog list.
12. System confirms the service was added.

---

## Alternative Flows

### AF-1: Edit Existing Service

**Branches from:** Main Flow step 3
**Condition:** Store owner wants to modify an existing service

1. Store owner clicks "Edit" on an existing service in the list.
2. System displays the service details form with current values pre-filled.
3. Store owner modifies the service name, base price, or other editable fields.
4. Store owner clicks "Save Changes".
5. System validates the input.
6. System updates the service record.
7. System confirms the changes and returns to the catalog list.
8. Use case ends.

### AF-2: Delete Service

**Branches from:** Main Flow step 3
**Condition:** Store owner wants to remove a service from the catalog

1. Store owner clicks "Delete" on a service in the list.
2. System displays a confirmation dialog: "Deleting this service will not affect existing orders. Continue?"
3. Store owner confirms deletion.
4. System marks the service as inactive/deleted or removes it from the active catalog.
5. System confirms the deletion and returns to the catalog list.
6. Use case ends.

### AF-3: Missing Required Fields

**Branches from:** Main Flow step 10
**Condition:** Service name or base price is empty

1. System displays an error message highlighting which fields are required.
2. Store owner fills in the missing fields.
3. Returns to Main Flow step 10.

### AF-4: Invalid Price Format

**Branches from:** Main Flow step 10
**Condition:** Base price is not a valid positive number

1. System displays an error message: "Base price must be a positive number."
2. Store owner corrects the price.
3. Returns to Main Flow step 10.

### AF-5: Configure Rush Pricing

**Branches from:** Main Flow step 11
**Condition:** Store owner wants to set rush/express multiplier

1. Store owner clicks "Configure Pricing" on a service.
2. System displays pricing configuration options.
3. Store owner can view/modify the rush multiplier (default 1.5x).
4. Store owner clicks "Save Pricing".
5. System updates the service pricing.
6. Use case ends.

---

## Postconditions

- **On success:** Service is created, updated, or deleted. Changes are persisted and immediately available in the service catalog for cashiers to use when creating new orders.
- **On failure:** Service is not created or modified. Store owner sees an error message and can retry.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Service name is mandatory and must be unique |
| BR-02 | Service type is mandatory and cannot be changed after creation |
| BR-03 | Base price is mandatory and must be a positive number |
| BR-04 | Weight-based services use kg as the unit; item-based services use pcs as the unit |
| BR-05 | Rush/express multiplier for all services is 1.5x by default and is globally configurable |
| BR-06 | Deleted/inactive services do not appear in the cashier's service selection but historical orders remain intact |
| BR-07 | Only store owners can add, edit, or delete services |

---

## Tests

> Tests verify the flows and business rules above. There is no separate acceptance-criteria list — the flows and rules *are* the acceptance criteria.

- [ ] Main Flow covered (steps 1–12)
- [ ] AF-1, AF-2, AF-3, AF-4, AF-5 covered
- [ ] BR-01 through BR-07 covered

---

## UI Surface

> What the user sees and where they reach it. Keep this implementation-agnostic — no framework annotations, component class names, or file paths.

- **Service Catalog List:** Table showing service name, type, base price, unit; "Add Service", "Edit", "Delete", and "Configure Pricing" buttons per service.
- **Add/Edit Service Form:** Text input for service name, dropdown for service type, number input for base price, read-only unit field based on type.
- **Delete Confirmation Dialog:** Warning message, "Confirm" and "Cancel" buttons.
- **Pricing Configuration Screen:** Display of rush multiplier, option to modify, "Save" button.

| Page | Access |
|------|--------|
| Service Catalog Management | Store Owner |
| Add/Edit Service Form | Store Owner |
| Pricing Configuration | Store Owner |
