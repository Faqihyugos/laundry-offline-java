# UC-004: Print Thermal Receipt and Share via WhatsApp

**Goal:** As a cashier or store owner, I want to print physical receipts on a thermal printer (58mm or 80mm) and send digital receipts directly to customers via WhatsApp so that customers have clear proof of their order, turnaround schedule, and payment details.

**Status:** Implemented
**Date:** 2026-11-19

> A use case cannot be marked as **Implemented** unless all criteria in the use-case implementation workflow are fulfilled.

---

## Actors

- **Primary actor:** Cashier
- **Secondary actor:** Store Owner
- **External recipient:** Customer (via WhatsApp)

---

## Preconditions

- User is authenticated and logged into the system
- An order exists with valid line items and payment details
- For physical printing: a printer (ESC/POS compatible or browser print dialog) is available
- For WhatsApp: customer has a phone number recorded

---

## Trigger

User clicks "Cetak Struk" (Print Receipt) or "Share WhatsApp" from the Order Creation confirmation dialog or Order Details view.

---

## Main Flow (Sub-flow A: Thermal Receipt Printing)

1. User clicks "Cetak Struk" on an order.
2. System retrieves order data (shop info from AppSetting, invoice number, date, customer name, line items, total, paid, remaining, payment status, estimated completion date, notes).
3. System formats the receipt layout tailored to the configured paper width (58mm or 80mm):
   - Shop Header (Laundry Name, Address, Phone)
   - Separator line (`--------------------------------`)
   - Invoice Number, Date/Time, Cashier Name
   - Customer Name & Phone
   - Separator line
   - Line items table: Service Name, Quantity, Unit, Rate, Subtotal
   - Separator line
   - Total Amount, Paid Amount, Change / Remaining Balance Due
   - Payment Status (`LUNAS` / `BELUM LUNAS` / `SEBAGIAN`) & Method (`CASH` / `TRANSFER`)
   - Estimated Completion Date (`Estimasi Selesai`)
   - Footer notes (e.g., "Terima kasih atas kepercayaan Anda", "Barang yang tidak diambil dalam 30 hari di luar tanggung jawab kami").
4. System triggers printing output (direct thermal ESC/POS commands or standard thermal print preview dialog).
5. User confirms print; receipt is printed.
6. System displays a confirmation notification.

---

## Main Flow (Sub-flow B: WhatsApp Digital Receipt Sharing)

1. User clicks "Share WhatsApp" on an order.
2. System formats a clean, readable digital receipt message text in Indonesian:
   ```
   *STRUK DIGITAL LAUNDRY - [Nama Laundry]*
   ----------------------------------------
   No. Invoice : [Invoice Number]
   Tanggal     : [Order Date]
   Pelanggan   : [Customer Name]
   Status      : [Order Status]
   Estimasi    : [Estimated Completion Date]
   ----------------------------------------
   *Rincian Layanan:*
   • [Service Name] ([Qty] [Unit]) : Rp [Subtotal]
   ----------------------------------------
   *Total*     : Rp [Total Amount]
   *Bayar*     : Rp [Paid Amount] ([Method])
   *Sisa*      : Rp [Remaining Amount]
   *Status Bayar*: [LUNAS / BELUM LUNAS / SEBAGIAN]
   ----------------------------------------
   Terima kasih telah mempercayakan pakaian Anda kepada kami!
   ```
3. System sanitizes customer's phone number into international format (e.g. `08123...` → `628123...`).
4. System constructs the WhatsApp URL: `https://wa.me/<sanitized_phone>?text=<url_encoded_message>`.
5. System opens the WhatsApp interface in a new window/tab or triggers the WhatsApp client.
6. User clicks send inside WhatsApp; customer receives the digital receipt.

---

## Alternative Flows

### AF-1: Share WhatsApp for Customer Without Phone Number
**Branches from:** Sub-flow B step 3
**Condition:** Customer has no phone number recorded
1. System alerts: "Pelanggan belum memiliki nomor telepon. Silakan lengkapi nomor telepon terlebih dahulu."
2. System provides a quick prompt to input customer's phone number.
3. User enters phone number and saves; system proceeds to send WhatsApp receipt.

### AF-2: Thermal Printer Paper Size Selection
**Branches from:** Sub-flow A step 3
**Condition:** User wants to switch between 58mm (narrow) and 80mm (wide) formats
1. In print preview, user can toggle between 58mm and 80mm format.
2. System reflows column widths and text alignment to match selected width.
3. User proceeds to print.

### AF-3: Reprinting Historical Receipt
**Branches from:** Trigger
**Condition:** Customer requests a receipt reprint days after original order creation
1. User searches and opens any completed or historical order in Order Management.
2. User clicks "Cetak Struk" or "Share WhatsApp".
3. System retrieves historical snapshot and generates receipt accurately reflecting current payment status.

---

## Postconditions

- **On success:** Physical thermal receipt is printed and/or digital WhatsApp message is sent to customer.
- **On failure:** Print error or phone validation error is presented to the user; order data remains intact.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Receipt formatting must support standard 58mm (approx. 32 characters per line) and 80mm (approx. 48 characters per line) character widths. |
| BR-02 | Receipt must prominently display the Invoice Number, Laundry Shop details, Payment Status, and Estimated Completion Date. |
| BR-03 | WhatsApp message link must correctly encode Indonesian characters, line breaks, and emojis/formatting asterisks. |
| BR-04 | Phone numbers must automatically convert local zero prefix (`08...`) to Indonesian country code (`628...`). |
| BR-05 | Struk must be reprintable at any point during or after order lifecycle. |

---

## Tests

- [x] Sub-flow A: Thermal receipt generation (58mm and 80mm) covered
- [x] Sub-flow B: WhatsApp digital receipt formatting and link generation covered
- [x] AF-1: Customer without phone prompt covered
- [x] AF-2: Paper width toggle covered
- [x] AF-3: Historical reprint covered
- [x] BR-01 through BR-05 covered

---

## UI Surface

- **Order Confirmation & Detail Buttons:**
  - "Cetak Struk" button (with printer icon).
  - "Share WhatsApp" button (with WhatsApp green brand icon).
- **Thermal Print Preview Dialog:**
  - Preview card showing monospace thermal slip representation.
  - Paper size selector (58mm / 80mm).
  - "Print" and "Cancel" buttons.

| Page / Component | Access |
|------------------|--------|
| Print Receipt Dialog | Cashier, Owner |
| WhatsApp Share Action | Cashier, Owner |
