# Design System

> Theme, component usage, and visual standards. Reference this when building or reviewing UI.

---

## 1. Theme & Brand Identity

- **Base theme:** Vaadin Aura
- **Custom CSS:** `src/main/resources/META-INF/resources/styles.css`
- **Application Brand Palette:** Modern Laundry Purple (from application design specifications)

**Aura and Lumo are two different, incompatible design systems.** This project uses **Aura**. Do not use `--lumo-*` CSS variables — they belong to the Lumo theme and must not be mixed with Aura. Use `--aura-*` variables for Aura-specific properties (typography, shadows) and `--vaadin-*` variables for base properties shared across all themes (spacing, radius, colors).

**Always use Aura theme variables instead of hard-coded values** (e.g., `--aura-font-size-xs` through `--aura-font-size-xl` for font sizes). Do not use hardcoded `px`, `rem`, or `em` values when an Aura variable exists.

---

## 2. Color Palette & Laundry Theme

### Brand Colors (Mapped to Aura Accent)
| Token / Property | Hex / Value | Usage |
|------------------|-------------|-------|
| `--aura-accent-color-light` | `#7B2D8E` | Primary purple brand color (buttons, active states, key highlights) |
| `--aura-accent-color-dark` | `#5A1D6B` | Dark purple variant for high contrast / dark mode |
| `primary-light` | `#9B4DB0` | Light accent for gradient headers & hover effects |
| `primary-surface` | `#F3E5F5` | Light purple background tint for active cards & chips |
| `secondary` | `#E1BEE7` | Subtle border and secondary accent |
| `secondary-light` | `#F8E8FC` | Very light tint for alternating rows / highlights |
| `--vaadin-background-color` | `#FAF7FB` | Soft neutral page canvas background |
| `--aura-surface-color-solid` | `#FFFFFF` | Clean white cards, modals, and container surfaces |

### Text Colors
| Role | Hex | Aura / Vaadin Equivalent |
|------|-----|--------------------------|
| Main Text (`textPrimary`) | `#1A1A2E` | `--vaadin-text-color` |
| Secondary Text (`textSecondary`) | `#6B6B7B` | `--vaadin-text-color-secondary` |
| Hint / Placeholder (`textHint`) | `#9E9E9E` | `--vaadin-text-color-disabled` |

### Status Badges & Indicators
Used for order workflow statuses (`Pending`, `Proses`, `Selesai`, `Diambil`) and payment statuses (`Lunas`, `Belum Lunas`, `Sebagian`):

| Status | Color | Hex | Usage |
|--------|-------|-----|-------|
| **Info / Pending** | Blue | `#2196F3` / `--aura-blue` | Order status: `Pending` |
| **Warning / In Progress** | Orange | `#FF9800` / `--aura-orange` | Order status: `Proses`; Payment: `Sebagian` |
| **Success / Completed** | Green | `#4CAF50` / `--aura-green` | Order status: `Selesai`; Payment: `Lunas` |
| **Neutral / Delivered** | Purple / Gray | `#7B2D8E` / `--aura-neutral` | Order status: `Diambil` |
| **Error / Alert** | Red | `#F44336` / `--aura-red` | Payment: `Belum Lunas`; overdue orders; errors |

---

## 3. Typography

- **App Font:** Instrument Sans (or Poppins fallback as per brand guidelines)
- Typography scale driven by `--aura-base-font-size` and `--aura-font-size-*` tokens:
  - Titles / Page Headers: `--aura-font-size-xl` (bold / semibold)
  - Card Titles & Section Headers: `--aura-font-size-l` (semibold)
  - Body Text & Form Labels: `--aura-font-size-m` (regular / medium)
  - Table Cells, Captions, Badges: `--aura-font-size-s` (medium)
  - Footnotes & Timestamps: `--aura-font-size-xs` (regular)

---

## 4. Spacing, Radius & Elevation

- **Spacing:** Use standard `--vaadin-gap-xs` through `-xl` and `--vaadin-padding-xs` through `-xl`.
- **Border Radius:**
  - Badges & Pills: Full radius (`9999px`)
  - Buttons & Inputs: `--vaadin-radius-m`
  - Cards, Dialogs & Modals: `--vaadin-radius-l`
- **Elevation:**
  - Cards & Panels: `--aura-shadow-s`
  - Overlays & Popups: `--aura-shadow-m`

---

## 5. Component Standards

| Component | Usage in Laundry App | Guidelines |
|-----------|----------------------|------------|
| `Grid` | Order list, Customer list, Service catalog, Sales reports | Column sorting, status badges, quick-action buttons (WA, Print) |
| `Button` | Primary CTAs (New Order, Save Customer, Export Excel, Print Receipt) | Use `ButtonVariant.LUMO_PRIMARY` (Aura accent) for main CTAs |
| `Dialog` | Quick modals for customer creation, payment settlement, printer config | Clean header, clear primary action, cancel option |
| `Select` / `ComboBox` | Service package selection, Customer autocomplete, Status dropdown | Clear labels, searchable for large lists |
| `TextField` / `NumberField` | Order quantity (kg/pcs), customer info, price inputs | Clear validation errors, thousand separators for currency |
| `Span` / Badge | Status indicators (`Pending`, `Proses`, `Selesai`, `Diambil`, `Lunas`) | Rounded pill badges with semantic status colors |
| `Tabs` | Dashboard & Report periods (Hari, Minggu, Bulan, Custom) | Easy switching between timeframe filters |

---

## 6. Responsive Behavior

- **Mobile Viewports (< 640px):**
  - Cashier counter-friendly: large touch targets for order item addition.
  - Stacked summary cards, bottom action bars for "Print Receipt" and "Share WhatsApp".
- **Tablet & Desktop (≥ 640px):**
  - Split layout for order entry: service selection on left, live order cart / ticket on right.
  - Side-by-side KPI cards on Dashboard and Report views.
