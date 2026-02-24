# CueAside

A minimalist Android app for digital wellbeing. Set notification routines that watch your app usage and fire timely alerts — your silent personal assistant for healthier phone habits.

## Features

- **Notification Routines**: Lightweight rules that combine app selection, conditions, icons, and messages
- **Three Trigger Types**:
  - When Launched — instant alert when opening an app
  - Time-based — after continuous or daily cumulative usage
  - When Exiting — alert when closing an app
- **Multi-app Support**: One routine can target multiple apps with the same condition
- **8 Themes**: Dark, Light, Aurora, Sunset, Ocean, Forest, Candy, Lava
- **Two Design Modes**: Bold (rounded, vibrant) or Minimal (flat, restrained)
- **Custom Icons**: Use app icons, preset emojis, or upload your own photos
- **Data Export**: Export all routines as JSON for backup or sharing
- **Permissions Manager**: Request and track Usage Access, Notifications, Battery optimization, and Boot completion

## Quick Start

### Web Interface (Testing & Preview)
1. Open `CueAside-FrontEnd.html` in a modern browser
2. Create routines using the intuitive multi-step interface
3. Switch between themes and design modes in Settings
4. Export routines as JSON

### Android Integration (Development)
CueAside is designed as a WebView-hosted frontend with a minimal Java bridge for system APIs.

**Recommended Architecture:**
```java
// In your Android Activity
WebView webView = findViewById(R.id.webview);
webView.addJavascriptInterface(new CueBridge(context), "CueBridge");
webView.loadUrl("file:///android_asset/CueAside-FrontEnd.html");
```

## Integration Guide

### Bridge Methods (Java ↔ JavaScript)

Your native code should expose these methods to the WebView:

#### getInstalledApps()
Returns JSON array of installed apps:
```json
[
  { "name": "Instagram", "pkg": "com.instagram.android", "iconBase64": "data:image/png;base64,..." },
  { "name": "Spotify", "pkg": "com.spotify.music", "iconBase64": "..." }
]
```

#### registerRoutine(jsonRoutine)
Called when user saves a routine. Store and activate it natively.
```json
{
  "id": "1234567890",
  "apps": [{"name": "Instagram", "pkg": "com.instagram.android", "icon": "📸"}],
  "cond": "used",
  "dur": 30,
  "unit": "m",
  "timeMode": "session",
  "icon": {"type": "preset", "e": "🔔"},
  "titleOn": true,
  "title": "Time Check",
  "msg": "You've been scrolling for 30 min!",
  "bubble": true,
  "enabled": true
}
```

#### Permission Request Methods
- `requestUsageAccess()` — launch ACTION_USAGE_ACCESS_SETTINGS
- `requestNotificationPermission()` — request POST_NOTIFICATIONS (Android 13+)
- `requestBatteryIgnore()` — request ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS

### Native Implementation Details

**Condition Triggers:**
- **When Launched**: Use `AccessibilityService` with `TYPE_WINDOW_STATE_CHANGED` events
- **Time-based**: 
  - *Session mode*: Track foreground time with `UsageStatsManager` + timer reset on app exit
  - *Total mode*: Use `UsageStatsManager.queryUsageStats(INTERVAL_DAILY)` for daily totals
- **When Exiting**: `AccessibilityService` window state change detection

**Notifications:**
- Use `NotificationCompat.Builder` for standard and bubble notifications
- `setContentTitle()` for the title (if titleOn = true)
- `setContentText()` for the message
- Call `setBubbleMetadata()` if bubble mode is enabled

**Permissions Required:**
```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
```

### Persistence Across Reboots
If routines should survive device reboots:
1. Add `RECEIVE_BOOT_COMPLETED` permission
2. Create a `BootCompleteReceiver` that re-registers all enabled routines
3. Store routine data in SharedPreferences or a Room database

## File Structure

```
CueAside/
├── CueAside-FrontEnd.html       # Complete UI (HTML + CSS + JS)
├── app/
│   ├── build.gradle              # Gradle config
│   └── src/                       # Your Java source files
├── gradle/                        # Gradle wrapper
├── build.gradle                   # Root gradle
├── settings.gradle
└── README.md
```

## Routine JSON Format

The frontend stores and exports routines in this format:
```json
{
  "id": "timestamp-string",
  "apps": [
    { "name": "Instagram", "pkg": "com.instagram.android", "icon": "📸" }
  ],
  "cond": "launched" | "used" | "exiting",
  "dur": 20,                       // Only for "used"
  "unit": "m" | "h" | "s",        // Only for "used"
  "timeMode": "session" | "total", // Only for "used"
  "icon": {
    "type": "app" | "preset" | "lib",
    "e": "🔔",                    // For preset
    "pkg": "...",                 // For app
    "idx": 0,                     // For lib
    "src": "base64-or-emoji"
  },
  "titleOn": true,
  "title": "Custom Title",
  "msg": "Notification message",
  "bubble": false,
  "enabled": true,
  "createdAt": 1708716428000
}
```

## Settings & Themes

### Available Themes
- **Default**: Dark blue accents
- **Light**: Clean white background
- **Aurora**: Purple & pink gradients
- **Sunset**: Warm orange tones
- **Ocean**: Cool cyan blues
- **Forest**: Natural greens
- **Candy**: Vibrant purples & pinks
- **Lava**: Fiery reds & oranges

### Design Modes
- **Minimal** (default): Flat, line-based, DM Sans font
- **Bold**: Rounded cards, vibrant gradients, Nunito font

## Technical Details

- **Frontend**: Vanilla JavaScript (no dependencies)
- **Storage**: LocalStorage (browser) or SharedPreferences (Android)
- **Version**: 1.5.0
- **Build Date**: 2026-02-21
- **Max Message Length**: 100 characters

## Development Notes

- The frontend includes TODO comments marked `TODO(java):` showing where native integration is needed
- Mock app list is hardcoded in the HTML for preview purposes
- In production, replace mock APPS array with real device apps from PackageManager
- Debug console available in Settings for troubleshooting
- All routine state is stored in `localStorage` (browser) or native storage (Android)

## License

This project is open source. See LICENSE file for details.