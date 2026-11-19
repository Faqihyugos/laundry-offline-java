# UC-002: Create Order

**Goal:** As a cashier, I want to create a new multi-item laundry order for a customer with estimated completion date and payment tracking (cash or transfer, full/unpaid/partial) so that incoming laundry is accurately recorded, tracked, and receipted.

**Status:** Implemented
**Date:** 2026-11-19

> A use case cannot be marked as **Implemented** unless all criteria in the use-case implementation workflow are fulfilled.

---

## Actors

- **Primary actor:** Cashier
- **Secondary actor:** Store Owner (can also create orders)

---

## Preconditions

- User is authenticated and logged into the system
- At least one active laundry service package exists in the system
- Customer exists or can be created on-the-fly during order creation

---

## Trigger

Cashier clicks "New Order" / "Create Order" button from the main navigation or dashboard.

---

## Main Flow

1. Cashier opens the Order Creation screen.
2. System automatically generates a unique Invoice Number using the configured invoice prefix and sequence (e.g. `LBW-YYYYMMDD-0001`).
3. Cashier selects an existing customer (via search by name or phone) or clicks "Add New Customer" to register a customer inline.
4. System sets the default order date/time to the current timestamp.
5. System displays available active service packages (weight-based `kg` or unit-based `pcs`).
6. Cashier adds one or more line items:
   - Selects a service package.
   - Enters quantity (decimal for kg, integer for pcs).
   - System calculates line subtotal = `quantity * price`.
   - Optionally adds item-specific notes (e.g., "baju putih dipisah", "noda di kerah").
7. System calculates order total amount = sum of all line item subtotals.
8. System automatically computes and suggests an Estimated Completion Date (`estimasi tanggal selesai`) based on the longest estimated duration among selected service packages, which cashier can adjust.
9. Cashier enters payment details:
   - Selects Payment Method: `Cash` (Tunai) or `Transfer` (Bank / E-Wallet).
   - Enters Paid Amount (`nominal bayar`).
   - System determines Payment Status:
     - If `paid_amount >= total_amount`: status is `Lunas` (calculates change if Cash).
     - If `paid_amount == 0`: status is `Belum Lunas`.
     - If `0 < paid_amount < total_amount`: status is `Sebagian` (calculates remaining balance due).
10. Cashier can enter general order notes (e.g. special handling, detergent preference).
11. Cashier clicks "Save Order" / "Proses Order".
12. System validates that the order has at least one item and customer is selected.
13. System saves the order with initial status `Pending`, along with its line items and payment record.
14. System displays order confirmation with options to:
    - Print Thermal Receipt (58mm/80mm) (see [UC-004](use-case-004-print-and-share-receipt.md)).
    - Send Digital Receipt via WhatsApp (see [UC-004](use-case-004-print-and-share-receipt.md)).
    - Create another order or view order details.

---

## Alternative Flows

### AF-1: Register New Customer Inline
**Branches from:** Main Flow step 3
**Condition:** Customer is not yet registered in the system
1. Cashier clicks "Quick Add Customer".
2. System opens a modal to input Name, Phone, and Address.
3. Cashier saves customer info; system creates customer and auto-selects them for the order.
4. Returns to Main Flow step 4.

### AF-2: Adjust Line Item Quantity or Remove Item
**Branches from:** Main Flow step 6
**Condition:** Cashier needs to correct a quantity or remove an added line item
1. Cashier edits quantity directly in the order cart or clicks "Remove" on a line item.
2. System recalculates item subtotal, total amount, estimated completion date, and payment balances.
3. Cashier resumes at Main Flow step 7.

### AF-3: Zero Initial Payment (Belum Lunas)
**Branches from:** Main Flow step 9
**Condition:** Customer chooses to pay later when laundry is finished or picked up
1. Cashier leaves Paid Amount as 0 or selects "Bayar Nanti".
2. System sets Payment Status to `Belum Lunas`, `paid_amount = 0`, and `remaining_amount = total_amount`.
3. Cashier proceeds to Main Flow step 11.

### AF-4: Partial Down Payment (Sebagian)
**Branches from:** Main Flow step 9
**Condition:** Customer pays a down payment (DP) less than the total amount
1. Cashier enters an amount less than `total_amount`.
2. System sets Payment Status to `Sebagian` and displays the remaining balance due (`remaining_amount = total_amount - paid_amount`).
3. Cashier proceeds to Main Flow step 11.

### AF-5: Cash Overpayment (Kembalian)
**Branches from:** Main Flow step 9
**Condition:** Customer pays Cash with a banknote higher than total amount
1. Cashier enters Cash amount (e.g. 100,000 for a 65,000 order).
2. System sets Payment Status to `Lunas` and displays change to be returned (`kembalian = paid_amount - total_amount`).
3. Cashier proceeds to Main Flow step 11.

---

## Postconditions

- **On success:** The order is stored with unique invoice number, line items, timestamps, estimated completion date, payment details, and initial status `Pending`. Customer transaction history reflects the new order.
- **On failure:** No order is saved; validation errors are highlighted; cashier remains on the draft order screen.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | An order must contain at least one service line item. |
| BR-02 | Service quantities must be positive (> 0); weight in kg supports decimals (e.g., 2.5 kg), pieces in pcs must be whole numbers. |
| BR-03 | Invoice number must be uniquely generated formatted with shop prefix (e.g., `{PREFIX}-{YYYYMMDD}-{SEQUENCE}`). |
| BR-04 | Initial order workflow status is always `Pending`. |
| BR-05 | Payment status must be accurately derived: `Lunas` (paid >= total), `Belum Lunas` (paid == 0), or `Sebagian` (0 < paid < total). |
| BR-06 | Payment method must be specified as `Cash` or `Transfer`. |
| BR-07 | Estimated completion date must be >= order date. |

---

## Tests

- [x] Main Flow covered (steps 1–14)
- [x] AF-1: Inline customer creation covered
- [x] AF-2: Line item removal and subtotal update covered
- [x] AF-3: Zero payment (Belum Lunas) covered
- [x] AF-4: Partial payment (Sebagian) covered
- [x] AF-5: Overpayment & change calculation (Lunas) covered
- [x] BR-01 through BR-07 covered

---

## UI Surface

- **Order Creation Screen:**
  - Left panel: Service package selector grid/buttons (categorized by unit kg / pcs) with search.
  - Right panel: Live Order Ticket/Cart showing Customer selector (with quick add), Invoice #, selected line items with quantity stepper, subtotal, total amount, estimated completion date picker, payment method toggle (`Cash` / `Transfer`), paid amount input, dynamic payment status badge (`Lunas` / `Belum Lunas` / `Sebagian`), change/remaining balance indicator, notes field, and "Proses Order" button.
- **Order Confirmation Dialog:** Order summary, Print Thermal Receipt button, Send WhatsApp Receipt button, and "New Order" / "View Orders" navigation.

| Page / Component | Access |
|------------------|--------|
| Order Creation | Cashier, Owner |
| Order Confirmation Dialog | Cashier, Owner |
