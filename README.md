# CueAside

CueAside is a small helper app concept that watches which app is in the foreground on Android and fires timely notifications based on simple conditions. Think of it as a silent personal assistant that taps you on the shoulder at the right moment.

## Key idea

A "routine" is a lightweight rule that watches one or more apps and fires a notification when a condition is met.

Routine = [App or Apps] + [Condition] + [Icon] + [Message]

One routine can target multiple apps with the same condition and message.

## Conditions (triggers)

- When Launched — notify as soon as the app is opened.
- Used for X time — notify after the app has been in the foreground for a specified duration (requires Usage Access).
- When Exiting — notify when the app is left (requires Accessibility Service).

## Integration overview (native <-> web UI)

The project is structured with a web frontend that already implements the full UI and routine JSON format. The cleanest native integration is a WebView host that exposes a small bridge for system calls:

Java example (WebView bridge):

webView.addJavascriptInterface(new CueBridge(context), "CueBridge");

Recommended bridge methods (examples used by the frontend):
- getInstalledApps() → JSON array of {name, pkg, iconBase64}
- registerRoutine(jsonRoutine) → store/register routine natively
- requestUsageAccess(), requestNotificationPermission(), requestBatteryIgnore()

On the native side you can implement the triggers using:
- UsageStatsManager.queryUsageStats() for foreground-time tracking
- AccessibilityService (TYPE_WINDOW_STATE_CHANGED) for launch/exit events
- NotificationCompat.Builder to post notifications

## Permissions and settings

- Usage Access — to measure time spent in apps
- Accessibility Service — to detect app window changes (launch/exit)
- Notification permission (POST_NOTIFICATIONS on newer Android versions)
- Ignore battery optimizations (optional) and BOOT completed handling if routines should persist across reboots

## Notes for developers

- The frontend UI stores routines as JSON. Keep the routine shape consistent so the native side can read and act on it.
- The frontend already includes mock app lists and TODOs where the Java bridge should supply real installed apps and icons.
- Prefer exposing minimal bridge methods — UI stays in HTML/JS, native code handles system APIs.

## License

Specify a license for the project (e.g. MIT) or add one to the repository.

---

This README is intentionally concise — it explains the concept, triggers, routine model, and the minimal integration points needed to connect the existing web UI to Android system services.