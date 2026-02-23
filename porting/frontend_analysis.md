# Frontend Analysis

## UI Components & Design Language

The app features two distinct design modes controlled by the `data-design` attribute:
1. **Design 1 (Bold):** Uses 'Nunito' font, rounded cards (16px), gradient FABs, and vibrant accents.
2. **Design 2 (Minimal):** Uses 'DM Sans', flat lines, subtle accents, and restrained typography (default).

### Key Components to Port

#### 1. Navigation
- **Bottom Navigation Bar:** 3 items (Create, List, Settings) with icons and labels.
- **Navigation Logic:** Simple tab switching between `<div class="screen">` elements.

#### 2. Create Routine Flow (Step-based)
- **Step 1: App Selection**
  - Search bar (client-side filtering).
  - List of apps with icons, names, and package names.
  - Multi-select vs Single-select toggle.
- **Step 2: Configuration**
  - Back button.
  - Condition selection (Launched, Used, Exiting).
  - Time-based settings (Session vs Total, duration input, unit dropdown).
  - Notification options (High priority, Timeout).
  - Icon picker (App icons vs Presets/Emojis).
  - Inputs for `cueName`, `title`, and `msg`.
  - Bubble notification toggle.
- **FAB:** Contextual "Next" or "Save" button.

#### 3. Routine List Screen
- **Cards:** Display routine icon, `cueName`, title, and condition summary.
- **Toggle:** Enable/disable routine without opening details.
- **Detail View:** Bottom sheet showing full routine details and a delete button.

#### 4. Settings Screen
- **Theming:** Grid of color swatches to switch between 8 themes (Dark, Light, Aurora, Sunset, Ocean, Forest, Candy, Lava).
- **Design Mode:** Toggle between "Bold" and "Minimal".
- **Permissions Status:** Rows with badges indicating status (Granted, Enable, Config).
- **App Data:** Rescan apps, Export JSON, About, Clear Data.

### Theming System (CSS Variables)
Themes are implemented using CSS variables (e.g., `--bg`, `--acc`, `--txt`).
- **Accent Colors:** Primary (`--acc`) and secondary/gradient (`--acc2`).
- **Backgrounds:** Graded levels (`--bg` to `--bg5`).
- **Status Colors:** Success, Warning, Danger.

## JavaScript State Management
- `ST` (State): Holds routines, icon library, and settings.
- `CR` (Create): Temporary state for the routine being created.
- `APPS`: Cached list of installed apps.

## UX Interactions
- **Bottom Sheets:** Used for detailed info and warnings.
- **Snackbars:** Brief success/error messages.
- **Plaster Overlay:** Loading transition when switching design modes.
- **Feedback:** Visual feedback on item selection (active/selected states).

## Porting Strategy to Compose
- **Themes:** Map CSS themes to Compose `ColorScheme` and `Typography`.
- **Design Modes:** Use custom `Shape` and `CompositionLocal` to toggle between "Bold" and "Minimal" styles.
- **Screens:** `NavHost` for tab navigation.
- **ViewModels:** Handle state (`ST`, `CR`, `APPS`) using `StateFlow`.
- **Lists:** `LazyColumn` for app list and routine list.
- **Components:** Recreate Bottom Bar, FAB, Cards, and Sheets using Material 3 and custom modifiers.
