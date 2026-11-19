# UC-001: Manage Customers

**Goal:** As a cashier or store owner, I want to manage customer records, view their transaction history, and communicate with them via WhatsApp or phone call so that customer relationship management and order processing are fast and personalized.

**Status:** Implemented
**Date:** 2026-11-19

> A use case cannot be marked as **Implemented** unless all criteria in the use-case implementation workflow are fulfilled.

---

## Actors

- **Primary actor:** Cashier
- **Secondary actor:** Store Owner (has same customer access rights)

---

## Preconditions

- User is authenticated and logged into the system (CASHIER or OWNER role)

---

## Trigger

User navigates to the Customer Management section or clicks "Add Customer" / "Customers" in the main navigation.

---

## Main Flow

1. User opens the Customer Management view.
2. System displays a searchable list of existing customers showing name, phone number, address, and total order count.
3. User clicks "Add New Customer".
4. System displays a customer creation form with fields: Full Name, Phone Number, and Address.
5. User enters customer full name, valid phone number (e.g., WhatsApp-compatible format), and address.
6. User clicks "Save Customer".
7. System validates that the name is provided and the phone number is valid and unique.
8. System saves the new customer profile and displays a success notification.
9. System updates the customer list and displays the newly created customer.

---

## Alternative Flows

### AF-1: Edit Existing Customer Details
**Branches from:** Main Flow step 2
**Condition:** User needs to update a customer's phone number, name, or address
1. User clicks "Edit" on a customer row in the list.
2. System opens the customer edit form populated with current details.
3. User modifies the information and clicks "Save Changes".
4. System validates the updated data and saves changes.
5. System displays a confirmation notification and updates the customer list.

### AF-2: View Customer Order History
**Branches from:** Main Flow step 2
**Condition:** User wants to see all orders previously placed by a customer
1. User clicks on a customer row or selects "View History / Detail".
2. System opens the Customer Detail view displaying customer info and a chronological list of all orders associated with the customer (invoice number, date, total amount, status, payment status).
3. User can click on any order in the history to view full order details.

### AF-3: Quick Action - WhatsApp Message
**Branches from:** Main Flow step 2 or AF-2 step 2
**Condition:** User wants to message the customer directly via WhatsApp
1. User clicks the WhatsApp quick action icon/button next to the customer's phone number.
2. System launches the WhatsApp chat interface using the pre-formatted WhatsApp URI (`https://wa.me/<phone_number>`).
3. User continues the conversation in WhatsApp.

### AF-4: Quick Action - Phone Call
**Branches from:** Main Flow step 2 or AF-2 step 2
**Condition:** User wants to call the customer by phone
1. User clicks the Phone quick action icon/button next to the customer's phone number.
2. System triggers a standard telephone call action (`tel:<phone_number>`).

### AF-5: Duplicate Phone Number
**Branches from:** Main Flow step 7 or AF-1 step 4
**Condition:** Another customer already has the entered phone number
1. System displays a validation error message: "Nomor telepon sudah terdaftar untuk pelanggan lain."
2. User edits the phone number or navigates to the existing customer's profile.
3. Returns to Main Flow step 5.

### AF-6: Delete Customer
**Branches from:** Main Flow step 2
**Condition:** User chooses to remove a customer who has no order history
1. User clicks "Delete" on a customer record.
2. If the customer has existing orders, system prevents deletion and alerts that customers with transaction history cannot be deleted.
3. If the customer has no orders, system prompts for confirmation.
4. User confirms deletion; system removes the customer record and confirms success.

---

## Postconditions

- **On success:** Customer is created, updated, or viewed. Transaction history is readily accessible, and quick communication actions (WhatsApp and Call) can be triggered.
- **On failure:** Customer record is not modified. Validation errors remain visible for correction.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Customer full name is mandatory (minimum 2 characters). |
| BR-02 | Customer phone number is mandatory, must be numeric/valid format, and must be unique across all customers. |
| BR-03 | Customer address is optional but recommended. |
| BR-04 | Customers with existing orders cannot be hard-deleted to preserve transaction integrity. |
| BR-05 | WhatsApp quick action must automatically sanitize phone numbers (e.g. converting leading '08' to international format '628'). |
| BR-06 | Customer search must allow searching by name or phone number with real-time filtering. |

---

## Tests

- [x] Main Flow covered (steps 1–9)
- [x] AF-1: Edit Customer covered
- [x] AF-2: View Order History covered
- [x] AF-3: Quick Action WhatsApp covered
- [x] AF-4: Quick Action Phone Call covered
- [x] AF-5: Duplicate Phone validation covered
- [x] AF-6: Delete Customer and constraint check covered
- [x] BR-01 through BR-06 covered

---

## UI Surface

- **Customer List Page:** Search input (by name or phone), "Add Customer" button, customer table/cards showing Name, Phone, Address, Order Count, Quick Actions (WhatsApp, Phone Call, Edit, Details).
- **Customer Form Dialog / Modal:** Text inputs for Name, Phone Number, and Address; Save and Cancel buttons.
- **Customer Detail / History Page:** Customer header with contact info and quick actions; historical orders table showing Invoice, Date, Items, Total, Order Status badge, Payment Status badge.

| Page / Component | Access |
|------------------|--------|
| Customer List | Cashier, Owner |
| Customer Form (Add/Edit) | Cashier, Owner |
| Customer Details & Order History | Cashier, Owner |
