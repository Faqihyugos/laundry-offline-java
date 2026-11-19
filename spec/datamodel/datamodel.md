# Data Model

> Entity definitions and relationships. Evolves as features are added.

## Entities

| Entity | Key Fields | Relationships |
|--------|-----------|------------------|
| **User** | id, username, password_hash, role (CASHIER, OWNER), created_at | Has many Orders (as cashier) |
| **Customer** | id, name, phone, email, created_date, total_spent, prepaid_balance | Has many Orders |
| **Service** | id, name, service_type (WEIGHT_BASED, ITEM_BASED), base_price, unit (kg or pcs), created_at | Has many OrderItems |
| **ServicePricing** | id, service_id, rush_multiplier (1.0x for normal, 1.5x for express), created_at | Belongs to Service |
| **Order** | id, customer_id, cashier_id, order_date, status (INTAKE, WASHING, DRYING, FOLDING, READY, PICKED_UP), total_amount, prepaid_applied, notes, created_at | Has many OrderItems, Belongs to Customer and User |
| **OrderItem** | id, order_id, service_id, quantity, unit_price, is_rush (boolean), subtotal, created_at | Belongs to Order and Service |
| **ConsumableInventory** | id, name, current_quantity, unit, low_stock_threshold, last_restocked_date, created_at | Tracks consumable stock levels |
| **Expense** | id, category, description, amount, expense_date, created_at | Tracks daily shop expenses |
| **Receipt** | id, order_id, printed_date, receipt_data (JSON), created_at | Stores receipt information for reprinting |

## Relationships Summary

- **User** (Cashier) → **Order** (one-to-many): One cashier processes many orders
- **Customer** → **Order** (one-to-many): One customer places many orders
- **Service** → **OrderItem** (one-to-many): One service type appears in many order items
- **Order** → **OrderItem** (one-to-many): One order contains multiple line items
- **ServicePricing** → **Service** (one-to-one): Each service has pricing configuration including rush multipliers

## Notes

- **Customer prepaid_balance:** Tracks prepaid deposits that can be applied to future orders
- **Order status workflow:** INTAKE → WASHING → DRYING → FOLDING → READY → PICKED_UP
- **OrderItem is_rush:** Boolean flag indicating whether rush/express pricing applies to that specific item
- **ServicePricing:** Stored separately to allow easy modification of rush multipliers without changing historical order data
- **Receipt:** Stores rendered receipt data to enable reprinting without re-querying order details
