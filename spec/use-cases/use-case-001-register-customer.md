# UC-001: Register Customer

> Copy this template for each feature as `use-case-NNN-short-name.md`.
> Replace all `[bracketed text]` with your content. Remove sections that genuinely do not apply (e.g. no secondary actor), but do not invent shortcuts — Preconditions, Trigger, Main Flow, and Postconditions are mandatory.

---

**Goal:** As a cashier, I want to register a new customer in the system so that I can create orders and track their transaction history and prepaid balance.

**Status:** Pending
**Date:** 2024-01-01

> A use case cannot be marked as **Implemented** unless all criteria in the use-case implementation workflow are fulfilled.

---

## Actors

- **Primary actor:** Cashier

---

## Preconditions

- Cashier is authenticated and logged into the system
- Cashier has CASHIER role permissions

---

## Trigger

Cashier clicks "Register New Customer" or accesses the customer registration screen.

---

## Main Flow

1. Cashier opens the customer registration form.
2. System displays a form with fields: name, phone, email.
3. Cashier enters customer's name.
4. Cashier enters customer's phone number.
5. Cashier enters customer's email (optional).
6. Cashier clicks "Register" or "Save".
7. System validates the input.
8. System creates a new customer record with prepaid_balance initialized to 0.
9. System displays a confirmation message with the customer ID.
10. System returns to the previous screen or customer details view.

---

## Alternative Flows

### AF-1: Duplicate Phone Number

**Branches from:** Main Flow step 7
**Condition:** A customer with the same phone number already exists

1. System displays an error message: "A customer with this phone number already exists."
2. Cashier can choose to view the existing customer or re-enter a different phone number.
3. Returns to Main Flow step 4.

### AF-2: Missing Required Fields

**Branches from:** Main Flow step 7
**Condition:** Name field is empty

1. System displays an error message highlighting which fields are required.
2. Cashier fills in the missing field.
3. Returns to Main Flow step 7.

### AF-3: Invalid Phone Format

**Branches from:** Main Flow step 7
**Condition:** Phone number format is invalid

1. System displays an error message: "Please enter a valid phone number."
2. Cashier corrects the phone number.
3. Returns to Main Flow step 7.

---

## Postconditions

- **On success:** A new customer record is created with the provided information and a prepaid_balance of 0.00. Cashier sees confirmation and can proceed to create orders for this customer.
- **On failure:** No customer record is created. The registration form remains open for the cashier to correct and retry.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Customer name is mandatory |
| BR-02 | Customer phone is mandatory and must be unique |
| BR-03 | Customer email is optional |
| BR-04 | New customers start with 0.00 prepaid balance |
| BR-05 | Phone number must be in a valid format (minimum 10 digits) |

---

## Tests

> Tests verify the flows and business rules above. There is no separate acceptance-criteria list — the flows and rules *are* the acceptance criteria.

- [ ] Main Flow covered (steps 1–10)
- [ ] AF-1, AF-2, AF-3 covered
- [ ] BR-01 through BR-05 covered

---

## UI Surface

> What the user sees and where they reach it. Keep this implementation-agnostic — no framework annotations, component class names, or file paths.

- Customer Registration Form: Text input fields for name, phone, email; a "Register" button; validation error messages displayed inline.
- Confirmation Dialog: Shows registered customer ID and option to register another customer or return to main menu.

| Page | Access |
|------|--------|
| Customer Registration | Cashier |
| Customer Confirmation | Cashier |
