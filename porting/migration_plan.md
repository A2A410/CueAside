# Kotlin & Jetpack Compose Migration Plan

## Phase 1: Environment & Architecture

### 1. Dependency Updates (`app/build.gradle`)
- Add Kotlin Gradle plugin.
- Add Compose BOM and dependencies (UI, Material 3, Tooling, Navigation).
- Add `Lifecycle` (ViewModel, Compose).
- Add `Hilt` for Dependency Injection (optional but recommended for scale).
- Replace `Gson` with `Kotlinx.Serialization`.
- Add `DataStore` for settings and routine persistence.

### 2. Architecture: MVVM
- **Model:** Kotlin Data Classes for `Routine`, `AppInfo`, `Settings`.
- **Repository:** `RoutineRepository` (Persistence) and `AppRepository` (Package Manager interaction).
- **ViewModel:**
  - `MainViewModel`: Global state (permissions, themes).
  - `CreateViewModel`: Step-based routine creation state.
  - `RoutineViewModel`: List management.
- **UI:** Compose Screens and Components.

## Phase 2: Core Logic Migration

### 1. Data Models (`foz.cueaside.aa.model`)
- Convert `Routine.java` to a Kotlin `data class`.
- Use sealed classes for `Condition` (Launched, Used, Exiting).
- Use an enum for `DesignMode` (Bold, Minimal).

### 2. Persistence Layer
- Implement `RoutineDataStore` or `RoutineDatabase` (Room).
- Migrate existing JSON from `SharedPreferences` to the new format if needed.

### 3. System Services
- **`AppTrackerService` (Kotlin):** Port `AccessibilityService` logic. Use Kotlin Coroutines for usage check delays.
- **`NotificationHelper` (Kotlin):** Port to Kotlin, utilizing modern Notification APIs.

## Phase 3: UI Migration (Jetpack Compose)

### 1. Theme Engine (`ui.theme`)
- Create `CueAsideTheme` that accepts `ThemeConfig` (color palette) and `DesignMode`.
- Implement dynamic `ColorScheme` based on the 8 themes.
- Implement conditional `Shapes` and `Typography` for Bold/Minimal modes.

### 2. Shared Components
- `CueBottomBar`: Native Bottom Navigation.
- `CueCard`: Themed card with toggle support.
- `CueBottomSheet`: For detail views and warnings.
- `CueFab`: Contextual floating action button.

### 3. Screen Implementations
- **`ListScreen`:** `LazyColumn` for routines.
- **`CreateScreen`:**
  - **Step 1:** App search and selection with `LazyColumn` and `SearchTextField`.
  - **Step 2:** Form for configuration using `OutlineTextField`, `Switch`, and `Dropdown`.
- **`SettingsScreen`:** List of settings items, theme picker grid, and permission status indicators.

## Phase 4: Integration & Cleanup

### 1. Navigation Setup
- Use `androidx.navigation.compose.NavHost`.
- Define routes: `LIST`, `CREATE`, `SETTINGS`.

### 2. Permission Handling
- Create a `PermissionManager` to handle checking and requesting system permissions in a Compose-friendly way.

### 3. Final Cleanup
- Remove `assets/index.html`, `style.css`, `bridge.js`.
- Remove `CueBridge.java`.
- Update `MainActivity` to use `setContent { ... }` instead of `setContentView(webView)`.

## UX Improvements during Migration
- **Animations:** Use Compose's `AnimatedContent` for screen transitions.
- **Performance:** Native app listing and icon loading will be much faster than base64 encoding/decoding.
- **Accessibility:** Better support for screen readers and system font scaling.
- **Stability:** Remove the "Accessibility unresponsive" bridge gap by using native observers.
