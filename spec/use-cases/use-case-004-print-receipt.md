# UC-004: Print Receipt

> Copy this template for each feature as `use-case-NNN-short-name.md`.
> Replace all `[bracketed text]` with your content. Remove sections that genuinely do not apply (e.g. no secondary actor), but do not invent shortcuts — Preconditions, Trigger, Main Flow, and Postconditions are mandatory.

---

**Goal:** As a cashier, I want to print a receipt summary for a customer so that they have a record of their order and transaction.

**Status:** Pending
**Date:** 2024-01-01

> A use case cannot be marked as **Implemented** unless all criteria in the use-case implementation workflow are fulfilled.

---

## Actors

- **Primary actor:** Cashier

---

## Preconditions

- Cashier is authenticated and logged into the system with CASHIER role
- An order exists and has been created (status is not null)
- A printer is configured and available on the local system

---

## Trigger

Cashier clicks "Print Receipt" on the order confirmation screen, order details view, or from the order list.

---

## Main Flow

1. Cashier selects an order and clicks "Print Receipt" or accesses the print option.
2. System retrieves the order details (customer info, service items, pricing, prepaid applied, total).
3. System generates a receipt layout with:
   - Shop name/header
   - Order ID and date/time
   - Customer name and phone
   - Service items (name, quantity, unit price, subtotal) with rush indicator if applicable
   - Subtotal, prepaid amount applied, final amount due
   - Notes if any
   - Footer with thank-you message
4. System displays a print preview (optional) for cashier to review.
5. Cashier clicks "Print" to send to the printer or confirms printing.
6. System sends the receipt to the configured local printer.
7. System displays a confirmation message: "Receipt printed successfully."
8. Receipt data is stored for future reprinting if needed.

---

## Alternative Flows

### AF-1: Printer Not Available

**Branches from:** Main Flow step 6
**Condition:** Printer is offline or not configured

1. System displays an error message: "Printer is not available. Please check printer configuration and try again."
2. Cashier can retry printing or save receipt data for later.
3. Returns to Main Flow step 5 or use case ends.

### AF-2: Print Preview Requested

**Branches from:** Main Flow step 4
**Condition:** Cashier chooses to preview the receipt

1. System displays a formatted receipt preview on screen.
2. Cashier reviews the content.
3. Cashier can click "Print" to proceed or "Cancel" to return.
4. Returns to Main Flow step 5.

### AF-3: Reprint Existing Receipt

**Branches from:** Main Flow step 1
**Condition:** Cashier wants to reprint a receipt for a previously created order

1. Cashier selects an order from the order history.
2. System retrieves the previously saved receipt data.
3. System displays the receipt and proceeds to Main Flow step 5 (Print).

---

## Postconditions

- **On success:** Receipt is sent to the printer. Receipt data is stored and can be retrieved for reprinting. Cashier sees a confirmation message.
- **On failure:** Receipt is not printed. Cashier sees an error message and can retry or save the receipt for later printing.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Receipt must include order ID, customer name, phone, and order date/time |
| BR-02 | Receipt must itemize all services with quantity, unit price, and subtotal |
| BR-03 | Receipt must display rush/express indicator if applicable to any item |
| BR-04 | Receipt must show prepaid amount applied and final amount due |
| BR-05 | Receipt must be printable to a local printer configured on the system |
| BR-06 | Receipt data is stored and can be retrieved for reprinting up to system retention policy |

---

## Tests

> Tests verify the flows and business rules above. There is no separate acceptance-criteria list — the flows and rules *are* the acceptance criteria.

- [ ] Main Flow covered (steps 1–8)
- [ ] AF-1, AF-2, AF-3 covered
- [ ] BR-01 through BR-06 covered

---

## UI Surface

> What the user sees and where they reach it. Keep this implementation-agnostic — no framework annotations, component class names, or file paths.

- **Print Receipt Button:** Located on order confirmation screen and order details view.
- **Print Preview (optional):** Formatted receipt layout showing all order details, with "Print" and "Cancel" buttons.
- **Print Confirmation Dialog:** Shows printer selection (if multiple printers available), "Print" and "Cancel" buttons.
- **Print Status Message:** Success or error message displayed after print attempt.

| Page | Access |
|------|--------|
| Order Confirmation | Cashier |
| Order Details | Cashier |
| Print Preview | Cashier |
| Print Status | Cashier |
