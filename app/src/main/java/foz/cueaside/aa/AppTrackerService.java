package foz.cueaside.aa;

import android.accessibilityservice.AccessibilityService;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class AppTrackerService extends AccessibilityService {
    private static final String TAG = "AppTrackerService";
    private static final String PREF_NAME = "CueAsidePrefs";
    private static final String KEY_LAST_EVENT = "last_acc_event";

    private RoutineManager routineManager;
    private String lastPackageName = "";
    private android.os.Handler usageHandler = new android.os.Handler();
    private static long lastEventTime = 0;
    private static AppTrackerService instance;

    public static long getLastEventTime() {
        return lastEventTime;
    }

    public static boolean isRunning() {
        return instance != null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        routineManager = new RoutineManager(this);
        Log.d(TAG, "Service Created");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Service Connected");
        updateHeartbeat();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        lastEventTime = System.currentTimeMillis();
        updateHeartbeat();

        try {
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                CharSequence pkg = event.getPackageName();
                String packageName = pkg != null ? pkg.toString() : "";

                // Ignore our own app and system UI usually, but for monitoring we need to know transitions
                if (!packageName.isEmpty() && !packageName.equals(lastPackageName)) {
                    Log.d(TAG, "App changed: " + lastPackageName + " -> " + packageName);
                    handleAppChange(lastPackageName, packageName);
                    lastPackageName = packageName;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onAccessibilityEvent: " + e.getMessage());
        }
    }

    private void updateHeartbeat() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putLong(KEY_LAST_EVENT, System.currentTimeMillis()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error updating heartbeat: " + e.getMessage());
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
                            Log.d(TAG, "Triggering Launch routine for: " + newPkg);
                            NotificationHelper.showNotification(this, r);
                            break;
                        }
                    }
                }

                // Check if routine targets the old app (Exit)
                if ("exiting".equals(r.cond)) {
                    for (Routine.AppInfo app : r.apps) {
                        if (app.pkg.equals(oldPkg)) {
                            Log.d(TAG, "Triggering Exit routine for: " + oldPkg);
                            NotificationHelper.showNotification(this, r);
                            break;
                        }
                    }
                }

                // Check for time-based (used)
                if ("used".equals(r.cond)) {
                    for (Routine.AppInfo app : r.apps) {
                        if (app.pkg.equals(newPkg)) {
                            if ("session".equals(r.timeMode)) {
                                scheduleUsageCheck(r);
                            } else if ("total".equals(r.timeMode)) {
                                checkTotalUsage(r, newPkg);
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in handleAppChange: " + e.getMessage());
        }
    }

    private void scheduleUsageCheck(Routine r) {
        try {
            long delayMillis = r.dur * 60 * 1000L;
            if ("h".equals(r.unit)) delayMillis *= 60;
            if ("s".equals(r.unit)) delayMillis = r.dur * 1000L;

            Log.d(TAG, "Scheduling usage check for " + r.id + " in " + delayMillis + "ms");
            usageHandler.postDelayed(() -> {
                try {
                    NotificationHelper.showNotification(this, r);
                } catch (Exception e) {
                    Log.e(TAG, "Error showing scheduled notification: " + e.getMessage());
                }
            }, delayMillis);
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling usage check: " + e.getMessage());
        }
    }

    private void checkTotalUsage(Routine r, String pkg) {
        try {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) return;

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long start = calendar.getTimeInMillis();
            long end = System.currentTimeMillis();

            Map<String, UsageStats> stats = usm.queryAndAggregateUsageStats(start, end);
            if (stats != null && stats.containsKey(pkg)) {
                long totalTimeMs = stats.get(pkg).getTotalTimeInForeground();
                long thresholdMs = r.dur * 60 * 1000L;
                if ("h".equals(r.unit)) thresholdMs *= 60;
                if ("s".equals(r.unit)) thresholdMs = r.dur * 1000L;

                if (totalTimeMs >= thresholdMs) {
                    NotificationHelper.showNotification(this, r);
                } else {
                    long remainingMs = thresholdMs - totalTimeMs;
                    usageHandler.postDelayed(() -> checkTotalUsage(r, pkg), Math.max(1000, remainingMs));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking total usage: " + e.getMessage());
        }
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.d(TAG, "Service Destroyed");
    }
}
