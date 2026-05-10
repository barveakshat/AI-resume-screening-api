# UI Design System Specification

## 1. Global Theme & Styling
- **Theme:** Dark mode only, modern SaaS dashboard aesthetic.
- **Typography:** Clean, sans-serif font family (e.g., Inter, Roboto).
- **Border Radius:** Soft rounded corners on all interactive elements and cards (approx. 6px to 8px).
- **Spacing:** Generous padding within cards and sections; clear visual hierarchy.

## 2. Color Palette
- **App Background:** Very dark navy/charcoal (approx. `#16181d` to `#1e212b`).
- **Sidebar Background:** Slightly darker than the main background to create depth.
- **Card Background:** Slightly lighter than the app background (approx. `#232733`).
- **Primary Accent:** Soft indigo/purple (used for primary CTA buttons, active states, and checkmarks).
- **Text Primary:** Bright white for headings and emphasized data.
- **Text Secondary:** Muted light grey for descriptions, timestamps, and inactive elements.
- **Status Accents:** Red for alerts/ongoing status dots; Green/Grey for neutral or resolved states.
- **Borders:** Very subtle, low-opacity white/grey for dividing lines and card outlines.

## 3. Layout Structure
- **Global Navigation (Far Left):** A narrow, vertical strip containing small utility icons.
- **Primary Sidebar (Left):**
  - Contains sections for main navigation.
  - Links structured with an icon on the left and text on the right.
  - Selected state: Highlighted with a subtle background color and brighter text.
  - Features a badge capability (e.g., a pill showing a number) next to navigation items.
  - User profile or workspace toggle located at the bottom.
- **Main Content Area (Right):** Takes up the remaining viewport width.

## 4. Main Content Components

### A. Page Header
- **Greeting/Title:** Large, prominent heading (H1) aligned to the left.
- **Search Bar:** Centered or right-aligned relative to the title. Contains a search icon and a visual keyboard shortcut indicator (e.g., `/`). Dark background, subtle border.
- **Primary Action Button:** Positioned on the far right. Indigo/purple background, white text, no border.

### B. Tab Navigation
- Positioned below the header.
- Text-based tabs with an icon. 
- Active tab has brighter text; inactive tabs use secondary text color.

### C. Data List / Table Card
- **Container:** Rounded dark card with a very subtle border.
- **Header Row:** Small, uppercase, secondary text defining columns.
- **List Items:**
  - Left side: Status icon (e.g., a shield or document inside a rounded square).
  - Main column: Bold primary text title, with smaller secondary text underneath for description.
  - Metadata columns: Right-aligned details (e.g., timestamps, durations).
  - Status indicator: A small colored dot (e.g., red) next to status text.
  - Action menu: Ellipsis (`...`) on the far right.
  - Hover state: Row background slightly lightens on hover.

### D. Expandable Task/Onboarding List
- **Container:** Rounded dark card.
- **Header:** Title on the left, progress indicator (e.g., "X out of Y steps") on the right.
- **List Items:**
  - Collapsed state: Circle icon (empty outline or filled checkmark), title text, right-facing chevron on the far right.
  - Expanded state:
    - Expanded background matches the card.
    - Contains title, secondary description text, and an action button (outline or solid).
    - Right side of the expanded area can contain a visual illustration or graphic block.
  - Dividers: Thin, subtle borders separate each item in the list.

## 5. Interaction & States
- **Hover Effects:** Subtle background color shifts on sidebar items, list rows, and buttons.
- **Focus States:** Distinct focus rings for accessibility on inputs and buttons.
- **Empty/Loading States:** Use skeletons or muted text where data is absent.
