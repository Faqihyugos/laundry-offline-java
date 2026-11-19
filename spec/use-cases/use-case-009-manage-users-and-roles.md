# UC-009: Manage Users and Roles

**Goal:** As a store owner, I want to manage user accounts (create, edit, deactivate cashiers and owners) and enforce role-based access control so that staff can only access permitted operations while administrative and financial functions remain secure.

**Status:** Implemented
**Date:** 2026-11-19

> A use case cannot be marked as **Implemented** unless all criteria in the use-case implementation workflow are fulfilled.

---

## Actors

- **Primary actor:** Store Owner

---

## Preconditions

- Store Owner is authenticated and logged into the system with OWNER role

---

## Trigger

Store Owner navigates to "Manajemen User" (User Management) under Settings / Admin menu.

---

## Main Flow

1. Owner opens the User Management screen.
2. System displays a list of existing user accounts with columns: Username, Full Name (Nama Lengkap), Role (`Owner` or `Kasir`), and Status (`Aktif` / `Nonaktif`).
3. Owner clicks "Tambah User" (Add User).
4. System displays a user creation form with fields:
   - Username (login identifier)
   - Nama Lengkap (Full name)
   - Password and Konfirmasi Password
   - Role (Dropdown: `Owner` or `Kasir`)
5. Owner enters unique username, full name, secure password, and selects role (`Kasir` for counter staff).
6. Owner clicks "Simpan User" (Save User).
7. System validates that the username is unique, passwords match, and fields are non-empty.
8. System securely hashes the password and creates the user account.
9. System displays a success notification and refreshes the user list.
10. The new user can immediately log in and will only have access according to their assigned role.

---

## Alternative Flows

### AF-1: Edit User Account / Change Password
**Branches from:** Main Flow step 2
**Condition:** Owner needs to update a user's name, role, or reset password
1. Owner clicks "Edit" on a user row.
2. System displays the user edit form populated with current details (password field blank unless being changed).
3. Owner updates full name, role, or provides a new password.
4. Owner clicks "Simpan Perubahan".
5. System validates and updates the user record.
6. System confirms success.

### AF-2: Deactivate User Account
**Branches from:** Main Flow step 2
**Condition:** Staff member has left employment
1. Owner clicks "Nonaktifkan" on a user account.
2. System displays confirmation prompt.
3. Owner confirms; system sets account status to inactive.
4. The deactivated user can no longer log in.

### AF-3: Prevent Deleting or Deactivating Own Account
**Branches from:** Main Flow step 2
**Condition:** Owner attempts to delete or deactivate their currently logged-in account
1. System disables the delete/deactivate action for the active session's user.
2. If attempted, system alerts: "Anda tidak dapat menonaktifkan atau menghapus akun Anda sendiri yang sedang aktif."

### AF-4: Unauthorized Access Attempt by Cashier
**Branches from:** Trigger
**Condition:** A user with role KASIR attempts to navigate to the User Management URL / view
1. System security interceptor blocks access.
2. System displays an "Akses Ditolak" (Access Denied / 403) notice and redirects the Cashier to the Order Management or Counter screen.

---

## Postconditions

- **On success:** User account is created, updated, or deactivated. Access rights are strictly enforced based on the assigned role:
  - `OWNER`: All features (Dashboard, Orders, Customers, Services, Sales Reports with Excel export, Settings, User Management).
  - `KASIR`: Orders, Customers, Service viewing/selection, Receipt printing, and WhatsApp receipt sharing. No access to User Management, Settings, or detailed Sales/Financial reports.
- **On failure:** User account is not modified. Validation errors remain visible.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | Only users with OWNER role have access to User Management and role assignment. |
| BR-02 | Username must be unique across all accounts, alphanumeric, minimum 4 characters. |
| BR-03 | Passwords must be securely hashed before storage (e.g. BCrypt / SHA-256). |
| BR-04 | The system must always maintain at least one active user with the OWNER role. |
| BR-05 | Users with the KASIR role are strictly prohibited from viewing detailed sales reports, financial figures, system settings, and user management. |
| BR-06 | Deactivated users cannot log into the system under any circumstance. |

---

## Tests

- [x] Main Flow covered (steps 1–10)
- [x] AF-1: Edit user & password reset covered
- [x] AF-2: Deactivate user covered
- [x] AF-3: Self-deactivation prevention covered
- [x] AF-4: Role enforcement & cashier access prevention covered
- [x] BR-01 through BR-06 covered

---

## UI Surface

- **User Management Page:**
  - Header with "Tambah User" button.
  - Table of users: Username, Nama Lengkap, Role badge (`Owner` [Purple] / `Kasir` [Blue]), Status badge, Actions (Edit, Deactivate).
- **User Form Dialog:**
  - Fields: Username, Nama Lengkap, Role selector (`Owner` / `Kasir`), Password, Konfirmasi Password.
  - Action buttons: "Simpan" and "Batal".

| Page / Component | Access |
|------------------|--------|
| User Management List | Store Owner |
| User Form Dialog | Store Owner |
| Access Denied View | Authenticated (Non-owner) |
