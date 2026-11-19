# Data Model

> Entity definitions and relationships. Evolves as features are added.

## Entities

| Entity | Key Fields | Relationships | Description |
|--------|------------|---------------|-------------|
| **User** | `id`, `username`, `password_hash`, `full_name`, `role` (`OWNER`, `CASHIER`), `created_at`, `updated_at` | Has many `Order`s | Stores user accounts for authentication and authorization. |
| **Customer** | `id`, `name`, `phone`, `address`, `created_at`, `updated_at` | Has many `Order`s | Stores customer profiles for order placement, order history, and WhatsApp/phone contact. |
| **Service** | `id`, `name`, `price`, `unit` (`KG`, `PCS`), `estimated_duration` (hours), `is_active` (boolean), `created_at`, `updated_at` | Has many `OrderItem`s | Laundry service packages offered by the shop with pricing and turnaround estimates. |
| **Order** | `id`, `invoice_number`, `customer_id`, `cashier_id`, `order_date`, `estimated_completion_date`, `status` (`PENDING`, `PROSES`, `SELESAI`, `DIAMBIL`), `payment_status` (`LUNAS`, `BELUM_LUNAS`, `SEBAGIAN`), `payment_method` (`CASH`, `TRANSFER`), `total_amount`, `paid_amount`, `remaining_amount`, `notes`, `created_at`, `updated_at` | Belongs to `Customer`, Belongs to `User` (cashier), Has many `OrderItem`s, Has many `Payment`s | Main order header representing laundry transactions, workflow lifecycle, and financial settlement. |
| **OrderItem** | `id`, `order_id`, `service_id`, `service_name`, `unit`, `unit_price`, `quantity`, `subtotal`, `notes` | Belongs to `Order`, Belongs to `Service` | Line item for an order specifying service, quantity (weight or count), rate, and subtotal. |
| **Payment** | `id`, `order_id`, `amount`, `payment_method` (`CASH`, `TRANSFER`), `payment_date`, `notes`, `created_at` | Belongs to `Order` | Tracks payment transactions made towards an order (supporting down-payments and settlement). |
| **AppSetting** | `id`, `setting_key`, `setting_value`, `updated_at` | None | Key-value store for application configuration: laundry info, invoice prefix, printer configuration. |
| **Promo** *(Optional / Extension)* | `id`, `name`, `discount_percent`, `start_date`, `end_date`, `is_active` | None | Promotional discount campaigns applied to orders. |

## Relationships Summary

- **User** (Cashier) → **Order** (one-to-many): Each order records the cashier who processed it.
- **Customer** → **Order** (one-to-many): One customer can have multiple laundry orders over time.
- **Order** → **OrderItem** (one-to-many): An order contains one or more line items (e.g. 5 kg Cuci Kering + 2 pcs Bedcover).
- **Service** → **OrderItem** (one-to-many): A service package can be referenced across multiple order items.
- **Order** → **Payment** (one-to-many): An order can receive multiple payments (e.g. partial upfront payment and final payment upon pickup).

## Enumerations & Status Workflows

### Order Status Workflow
```
[ PENDING ] ──> [ PROSES ] ──> [ SELESAI ] ──> [ DIAMBIL ]
```
- **PENDING:** Order received/registered at counter; waiting for washing/processing to begin.
- **PROSES:** Order is actively being washed, dried, or ironed.
- **SELESAI:** Laundry is clean, folded, packaged, and ready for pickup.
- **DIAMBIL:** Order has been collected by the customer.

### Payment Status
- **LUNAS (Paid in Full):** `paid_amount` >= `total_amount`, `remaining_amount` = 0.
- **BELUM_LUNAS (Unpaid):** `paid_amount` = 0, `remaining_amount` = `total_amount`.
- **SEBAGIAN (Partially Paid):** 0 < `paid_amount` < `total_amount`, `remaining_amount` > 0.

### Payment Methods
- **CASH (Tunai)**
- **TRANSFER (Bank / E-Wallet Transfer)**

### Service Units
- **KG (Kilogram):** Weight-based services (e.g. Cuci Kering Reguler, Cuci Setrika Express).
- **PCS (Pieces / Satuan):** Item-based services (e.g. Bedcover, Jas, Selimut, Sepatu).

### App Settings Keys
- `laundry_name`: Name of the laundry business (Default: "Laundry Bersih Wangi")
- `laundry_address`: Physical address (Default: "Jl. Sudirman No. 123, Jakarta")
- `laundry_phone`: Contact telephone / WhatsApp number (Default: "6281234567890")
- `invoice_prefix`: Invoice code prefix (Default: "LBW")
- `printer_paper_size`: Thermal paper width (e.g. "58mm", "80mm")
