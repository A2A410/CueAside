# Source Code Analysis

## Architecture Overview
The current app follows a Hybrid architecture where the UI is rendered in a `WebView` and logic is partially handled in Java through a `JavascriptInterface` named `CueBridge`.

## Core Components

### 1. `MainActivity.java`
- **Role:** Entry point, hosts `WebView`.
- **Logic:**
  - Initializes `WebView` and `CueBridge`.
  - Handles basic permission requests (Notification permission for API 33+).
  - Triggers permission checks in JS when resumed.

### 2. `CueBridge.java`
- **Role:** Bridge between JS and Java.
- **Exposed Methods:**
  - `getRoutines()`: Returns routines as JSON.
  - `saveRoutine(json)`: Deserializes and saves a routine.
  - `deleteRoutine(id)`, `toggleRoutine(id, enabled)`: Routine management.
  - `getSettings()`, `saveSettings(json)`: Settings persistence.
  - `getApps()`: Fetches installed apps, including base64 encoded icons.
  - `checkPermissionsStatus()`: Checks for Usage Stats, Accessibility, and Notification permissions.
  - `requestUsageAccess()`, `requestAccessibilitySettings()`, `requestNotificationPermission()`, `requestBatteryIgnore()`: Intents to system settings.
  - `openAppInfo()`: Opens app settings for Force Stop (recovery from Accessibility bug).
  - `refreshApps()`: Background thread to fetch apps and notify JS.

### 3. `AppTrackerService.java`
- **Role:** `AccessibilityService` that monitors app lifecycle.
- **Logic:**
  - `onAccessibilityEvent`: Listens for `TYPE_WINDOW_STATE_CHANGED` to detect app launches/exits.
  - `handleAppChange`: Triggers notifications based on routine conditions:
    - `launched`: Immediate notification.
    - `exiting`: Notification when app is closed.
    - `used`:
      - `session`: Schedules a notification after a delay.
      - `total`: Uses `UsageStatsManager` to check 24h usage and schedules check if threshold isn't met yet.

### 4. `RoutineManager.java`
- **Role:** Persistence layer using `SharedPreferences` and `Gson`.
- **Keys:** `routines`, `settings`.

### 5. `NotificationHelper.java`
- **Role:** Displays notifications.
- **Features:**
  - Notification Channels (Default vs High Priority).
  - Automatic withdrawal (timeout) via `Handler.postDelayed`.
  - Prefixing title with `cueName`.

### 6. `Routine.java`
- **Role:** Data model.
- **Fields:** `id`, `seqId`, `cueName`, `apps`, `cond`, `dur`, `unit`, `timeMode`, `icon`, `title`, `msg`, `bubble`, `enabled`, `highPriority`, `timeout`.

## System Integrations
- **Accessibility Service:** Critical for real-time app tracking.
- **Usage Stats Manager:** Used for daily usage tracking.
- **Notification Manager:** For firing alerts.
- **Package Manager:** For listing installed apps and icons.

## Key Logic to Port
1. **App Discovery:** Efficiently fetching and caching app list + icons.
2. **Permission Workflow:** State machine for various system permissions.
3. **Routine Matching Engine:** Logic in `AppTrackerService` needs to be kept but better integrated with the Kotlin models.
4. **Persistence:** Migrate from `SharedPreferences` + `Gson` to `DataStore` or `Room`.
