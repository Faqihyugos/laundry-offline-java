# Project Context

> High-level context for the project: the problem being solved, who it's for, what's in scope, and what constraints apply.

## 1. Vision

Laundry Express is an offline-ready point-of-sale and operations management system designed for small to medium-sized laundry shops. It enables cashiers to quickly process customer orders with flexible service options (weight-based and item-based pricing) while allowing store owners to maintain control over pricing catalogs, monitor inventory, track expenses, and understand business performance through financial dashboards. The system prioritizes operational efficiency and must function reliably even when internet connectivity is unavailable.

## 2. Users

### Cashiers
Cashiers are front-line staff who interact directly with customers. They can:
- Register new customers in the system
- Create and manage orders combining weight-based services (measured in kg) and item-based services (counted in pieces)
- Apply rush/express pricing options for expedited service
- Update order status as laundry moves through workflow stages (intake, washing, drying, folding, ready, picked-up)
- Apply prepaid customer deposits to settle orders
- Print receipt summaries for customers

### Store Owners
Store owners manage the business operations. They can:
- Configure service catalogs (define what services are offered)
- Set pricing for weight-based and item-based services
- Monitor consumable inventory levels (e.g., detergent, fabric softener) with low-stock warnings
- Log daily shop expenses (utilities, supplies, maintenance)
- Review financial dashboards showing revenue, expenses, and profitability
- Review order summary dashboards showing order counts, status distribution, and customer insights

## 3. Constraints

- **Offline-first:** The system must operate without network connectivity; synchronization is not in scope for initial release
- **Local storage only:** All data is stored locally; no external database integrations
- **Role-based access control:** Authentication and authorization distinguish between cashiers and store owners
- **Receipt printing:** System must support printing receipt summaries to local printers
- **No external integrations:** Payment processing, SMS, email, or cloud sync are out of scope

---

# Related Documents

- [Spec README](README.md) — process overview and workflow
- [Architecture](architecture.md) — technology stack and application structure
- [Design System](design-system.md) — theme, component usage, and visual standards
- [Use Case Template](use-cases/use-case-template.md) — template for feature specifications
