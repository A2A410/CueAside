package foz.cueaside.aa;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import java.util.List;

public class AppTrackerService extends AccessibilityService {
    private static final String TAG = "AppTrackerService";
    private RoutineManager routineManager;
    private String lastPackageName = "";
    private android.os.Handler usageHandler = new android.os.Handler();
    private Runnable usageRunnable;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Service Connected");
        routineManager = new RoutineManager(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
                if (!packageName.equals(lastPackageName)) {
                    handleAppChange(lastPackageName, packageName);
                    lastPackageName = packageName;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onAccessibilityEvent: " + e.getMessage());
        }
    }

    private void handleAppChange(String oldPkg, String newPkg) {
        try {
            usageHandler.removeCallbacksAndMessages(null);
            List<Routine> routines = routineManager.getRoutines();
            if (routines == null) return;
            for (Routine r : routines) {
            if (!r.enabled) continue;

            // Check if routine targets the new app (Launch)
            if ("launched".equals(r.cond)) {
                for (Routine.AppInfo app : r.apps) {
                    if (app.pkg.equals(newPkg)) {
                        NotificationHelper.showNotification(this, r.title, r.msg);
                        break;
                    }
                }
            }

            // Check if routine targets the old app (Exit)
            if ("exiting".equals(r.cond)) {
                for (Routine.AppInfo app : r.apps) {
                    if (app.pkg.equals(oldPkg)) {
                        NotificationHelper.showNotification(this, r.title, r.msg);
                        break;
                    }
                }
            }

            // Check for time-based (used)
            if ("used".equals(r.cond)) {
                for (Routine.AppInfo app : r.apps) {
                    if (app.pkg.equals(newPkg)) {
                        scheduleUsageCheck(r);
                        break;
                    }
                }
            }
        }
    }

    private void scheduleUsageCheck(Routine r) {
        try {
            long delayMillis = r.dur * 60 * 1000; // default min
            if ("h".equals(r.unit)) delayMillis *= 60;
            if ("s".equals(r.unit)) delayMillis = r.dur * 1000;

            if ("session".equals(r.timeMode)) {
                usageHandler.postDelayed(() -> {
                    try {
                        NotificationHelper.showNotification(this, r.title, r.msg);
                    } catch (Exception e) {
                        Log.e(TAG, "Error showing scheduled notification: " + e.getMessage());
                    }
                }, delayMillis);
            } else {
                // Total daily usage implementation would go here
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling usage check: " + e.getMessage());
        }
    }

    @Override
    public void onInterrupt() {
    }
}
