# UC-007: Log Daily Shop Expenses

> Copy this template for each feature as `use-case-NNN-short-name.md`.
> Replace all `[bracketed text]` with your content. Remove sections that genuinely do not apply (e.g. no secondary actor), but do not invent shortcuts — Preconditions, Trigger, Main Flow, and Postconditions are mandatory.

---

**Goal:** As a store owner, I want to log daily shop expenses (utilities, supplies, maintenance) so that I can track business costs and calculate profitability.

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

Store owner clicks "Expenses" or "Log Expense" in the owner dashboard or accounting section.

---

## Main Flow

1. System displays the Expense Management screen.
2. System shows a list of previously logged expenses with columns: date, category, description, amount.
3. Store owner clicks "Add Expense" or "Log New Expense".
4. System displays an expense entry form with fields: expense date (date picker), category (dropdown), description, amount.
5. Store owner selects the expense date using a date picker (default: today).
6. Store owner selects a category from a dropdown (e.g., Utilities, Supplies, Maintenance, Rent, Miscellaneous).
7. Store owner enters a description of the expense (e.g., "Water bill for January").
8. Store owner enters the expense amount as a positive number.
9. Store owner clicks "Save Expense".
10. System validates the input.
11. System creates the expense record and associates it with the entered date.
12. System displays a confirmation message.
13. System updates the expense list to show the newly added expense.

---

## Alternative Flows

### AF-1: Edit Existing Expense

**Branches from:** Main Flow step 3
**Condition:** Store owner wants to modify a previously logged expense

1. Store owner clicks "Edit" on an expense in the list.
2. System displays the expense form with current values pre-filled.
3. Store owner modifies the category, description, or amount.
4. Store owner clicks "Save Changes".
5. System validates the input.
6. System updates the expense record.
7. System confirms the changes and returns to the expense list.
8. Use case ends.

### AF-2: Delete Expense

**Branches from:** Main Flow step 3
**Condition:** Store owner wants to remove an incorrectly logged expense

1. Store owner clicks "Delete" on an expense in the list.
2. System displays a confirmation dialog: "Are you sure you want to delete this expense?"
3. Store owner confirms deletion.
4. System removes the expense record.
5. System confirms the deletion and returns to the expense list.
6. Use case ends.

### AF-3: View Expense Summary by Date Range

**Branches from:** Main Flow step 2
**Condition:** Store owner wants to filter expenses by date or category

1. Store owner clicks "Filter" or "Date Range" on the expense list.
2. System displays filter options: start date, end date, category.
3. Store owner selects a date range (e.g., January 1 – January 31).
4. Store owner optionally selects a specific category.
5. Store owner clicks "Apply Filter".
6. System displays expenses matching the selected criteria.
7. System shows a summary: total expenses for the period and breakdown by category (optional).
8. Use case ends.

### AF-4: Missing Required Fields

**Branches from:** Main Flow step 10
**Condition:** Description or amount field is empty

1. System displays an error message highlighting which fields are required.
2. Store owner fills in missing fields.
3. Returns to Main Flow step 10.

### AF-5: Invalid Amount Format

**Branches from:** Main Flow step 10
**Condition:** Amount is not a valid positive number

1. System displays an error message: "Amount must be a positive number."
2. Store owner corrects the amount.
3. Returns to Main Flow step 10.

---

## Postconditions

- **On success:** Expense is created, updated, or deleted. Changes are persisted and immediately reflected in the expense list and financial dashboards.
- **On failure:** Expense is not created or modified. Store owner sees an error message and can retry.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Expense category is mandatory (Utilities, Supplies, Maintenance, Rent, Miscellaneous, etc.) |
| BR-02 | Description is mandatory and should briefly explain the expense |
| BR-03 | Amount is mandatory and must be a positive number |
| BR-04 | Expense date is mandatory; default is today but can be changed |
| BR-05 | Expenses can be logged for any date, including past dates |
| BR-06 | Only store owners can log, edit, or delete expenses |
| BR-07 | Expenses are used to calculate net profit (revenue - expenses) |

---

## Tests

> Tests verify the flows and business rules above. There is no separate acceptance-criteria list — the flows and rules *are* the acceptance criteria.

- [ ] Main Flow covered (steps 1–13)
- [ ] AF-1, AF-2, AF-3, AF-4, AF-5 covered
- [ ] BR-01 through BR-07 covered

---

## UI Surface

> What the user sees and where they reach it. Keep this implementation-agnostic — no framework annotations, component class names, or file paths.

- **Expense List:** Table showing date, category, description, amount; "Add Expense", "Edit", and "Delete" buttons per row; "Filter" button for date range and category.
- **Add/Edit Expense Form:** Date picker, dropdown for category, text area for description, number input for amount, "Save" and "Cancel" buttons.
- **Delete Confirmation Dialog:** Warning message, "Confirm" and "Cancel" buttons.
- **Expense Filter Panel:** Date range pickers (start, end), category dropdown (multi-select or single), "Apply Filter" and "Reset" buttons.
- **Expense Summary:** Period display, total expenses, breakdown by category (optional).

| Page | Access |
|------|--------|
| Expense Management | Store Owner |
| Add/Edit Expense Form | Store Owner |
| Expense Filter | Store Owner |
| Expense Summary | Store Owner |
