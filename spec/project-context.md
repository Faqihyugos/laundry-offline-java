# Project Context

> High-level context for the project: the problem being solved, who it's for, what's in scope, and what constraints apply.

## 1. Vision

Laundry App is a comprehensive, offline-first Point-of-Sale (POS) and operations management system designed for laundry businesses. The system allows laundry operators to manage customer intake, process multi-item laundry orders (both weight-based in kg and item-based in pcs), track order statuses through a simple 4-stage lifecycle, handle cash/transfer payments (full, unpaid, or partial), print thermal receipts (58mm/80mm), share digital receipts to customers via WhatsApp, analyze sales trends with Excel export, and configure shop settings.

The application is built to run 100% offline with local data storage so that business operations never halt due to internet connectivity issues.

## 2. Users & Roles

The system supports two distinct roles with segregated access control:

| Role | Access Scope |
|------|--------------|
| **Owner** | Full access to all features: Dashboard, Order Management, Customer Management, Service Packages, Detailed Sales Reports with Excel Export, User Management, and Shop/Printer Settings. |
| **Kasir (Cashier)** | Counter operations: Create & manage orders, Customer database & history, Service package selection, Print receipts, Share WhatsApp receipts (no access to user management or detailed financial/sales reports). |

### Kasir (Cashier)
Front-counter staff responsible for daily transactions:
- **Customer Management:** Register, update, and search customers (name, phone number, address); view transaction history; trigger quick actions (direct WhatsApp chat, phone call).
- **Order Management:** Create orders with multiple items (weight in kg, pieces in pcs); assign estimated completion date; record payments (`Lunas`, `Belum Lunas`, `Sebagian`) with payment method (`Cash`, `Transfer`).
- **Order Workflow:** Update order status progressively (`Pending` → `Proses` → `Selesai` → `Diambil`).
- **Receipt Printing & Sharing:** Print physical thermal receipts (58mm/80mm) and send digital formatted receipts directly to customer via WhatsApp.

### Store Owner (Owner)
Business administrator responsible for operations, staff, and analytics:
- **Executive Dashboard:** View summary of today's revenue, order counts grouped by status, weekly revenue trend chart, and the 5 most recent orders.
- **Service Package Management:** CRUD laundry services (service name, price, unit [kg/pcs], estimated completion duration, active/inactive toggle).
- **Sales Reports & Analytics:** Analyze revenue and transaction volume across periods (day, week, month, or custom range); view sales trend charts; export reports to Excel (`.xlsx`).
- **User Management:** Create, edit, and manage accounts for Cashiers and Owners.
- **System Settings:** Configure laundry profile (name, address, phone), custom invoice number prefix (e.g. `LBW`), and thermal printer connection settings.

## 3. Core Modules & Features

1. **Dashboard**
   - Today's revenue summary
   - Total order count broken down by status (`Pending`, `Proses`, `Selesai`, `Diambil`)
   - Weekly revenue chart
   - List of 5 latest orders with quick status and details
2. **Order Management (Manajemen Order)**
   - Multi-item order creation with automatic subtotal and total calculations
   - Order status progression: `Pending` → `Proses` → `Selesai` → `Diambil`
   - Payment status tracking: `Lunas` (Paid), `Belum Lunas` (Unpaid), `Sebagian` (Partial)
   - Payment methods: `Cash`, `Transfer`
   - Estimated completion date tracking
   - Thermal receipt printing and WhatsApp digital receipt sharing
3. **Customer Management (Manajemen Pelanggan)**
   - Complete customer database: name, phone number, address
   - Full order and transaction history per customer
   - Quick communication actions: one-click WhatsApp message and telephone call
4. **Service Packages (Paket Layanan)**
   - Full CRUD for service packages
   - Service name, price, unit (`kg` or `pcs`), estimated turnaround duration
   - Active / Inactive status toggle
5. **Sales Reports (Laporan Penjualan)**
   - Flexible period filters: Daily (Hari), Weekly (Minggu), Monthly (Bulan), Custom date range
   - Total revenue and total transaction count
   - Visual sales trend chart
   - Export reports directly to Excel format (`.xlsx`)
6. **Settings & Configuration (Pengaturan)**
   - Laundry profile info: Shop name, address, contact phone number
   - Custom invoice number prefix (e.g. `LBW`, `INV`)
   - User account management (Owner only)
   - Thermal printer settings (58mm / 80mm format selection)
7. **Multi-User & Role-Based Access Control**
   - Secure authentication for Owner and Kasir
   - Strict role-based navigation and view authorization

## 4. Constraints

- **Offline-First:** All functionality runs locally without requiring an internet connection.
- **Local Persistence:** Data is stored locally within the application database.
- **WhatsApp Web / App Integration:** Digital receipts and customer quick actions use standardized URI schemes (`https://wa.me/<phone>?text=...` / `tel:<phone>`) requiring no external paid SMS/messaging gateway.
- **Thermal Printing Standard:** Formatted for standard 58mm and 80mm thermal receipt output.
- **Architecture & Tech Stack:** Implemented using Java / Spring Boot and Vaadin as specified in `architecture.md` (independent of mobile/Flutter technology).

---

# Related Documents

- [Spec README](README.md) — process overview and workflow
- [Architecture](architecture.md) — technology stack and application structure
- [Design System](design-system.md) — theme, component usage, and visual standards
- [Data Model](datamodel/datamodel.md) — entity definitions and relationships
- [Use Case Template](use-cases/use-case-template.md) — template for feature specifications
