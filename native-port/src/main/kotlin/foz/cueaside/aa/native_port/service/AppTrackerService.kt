package foz.cueaside.aa.native_port.service

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import foz.cueaside.aa.native_port.data.RoutineManager
import foz.cueaside.aa.native_port.model.Routine
import foz.cueaside.aa.native_port.util.NotificationHelper
import java.util.*

class AppTrackerService : AccessibilityService() {
    private lateinit var routineManager: RoutineManager
    private var lastPackageName: String = ""
    private val usageHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service Connected")
        routineManager = RoutineManager(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        lastEventTime = System.currentTimeMillis()
        try {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val packageName = event.packageName?.toString() ?: ""
                if (packageName != lastPackageName) {
                    handleAppChange(lastPackageName, packageName)
                    lastPackageName = packageName
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onAccessibilityEvent: ${e.message}")
        }
    }

    private fun handleAppChange(oldPkg: String, newPkg: String) {
        try {
            usageHandler.removeCallbacksAndMessages(null)
            val routines = routineManager.getRoutines()
            for (r in routines) {
                if (!r.enabled) continue

                // Check if routine targets the new app (Launch)
                if (r.cond == "launched") {
                    if (r.apps.any { it.pkg == newPkg }) {
                        NotificationHelper.showNotification(this, r)
                    }
                }

                // Check if routine targets the old app (Exit)
                if (r.cond == "exiting") {
                    if (r.apps.any { it.pkg == oldPkg }) {
                        NotificationHelper.showNotification(this, r)
                    }
                }

                // Check for time-based (used)
                if (r.cond == "used") {
                    if (r.apps.any { it.pkg == newPkg }) {
                        if (r.timeMode == "session") {
                            scheduleUsageCheck(r)
                        } else if (r.timeMode == "total") {
                            checkTotalUsage(r, newPkg)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleAppChange: ${e.message}")
        }
    }

    private fun scheduleUsageCheck(r: Routine) {
        try {
            var delayMillis = r.dur * 60 * 1000L
            if (r.unit == "h") delayMillis *= 60
            if (r.unit == "s") delayMillis = r.dur * 1000L

            usageHandler.postDelayed({
                try {
                    NotificationHelper.showNotification(this, r)
                } catch (e: Exception) {
                    Log.e(TAG, "Error showing scheduled notification: ${e.message}")
                }
            }, delayMillis)
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling usage check: ${e.message}")
        }
    }

    private fun checkTotalUsage(r: Routine, pkg: String) {
        try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = calendar.timeInMillis
            val end = System.currentTimeMillis()

            val stats = usm.queryAndAggregateUsageStats(start, end)
            if (stats.containsKey(pkg)) {
                val totalTimeMs = stats[pkg]?.totalTimeInForeground ?: 0L
                var thresholdMs = r.dur * 60 * 1000L
                if (r.unit == "h") thresholdMs *= 60
                if (r.unit == "s") thresholdMs = r.dur * 1000L

                if (totalTimeMs >= thresholdMs) {
                    NotificationHelper.showNotification(this, r)
                } else {
                    // Schedule a check for when the threshold will be reached
                    val remainingMs = thresholdMs - totalTimeMs
                    usageHandler.postDelayed({ checkTotalUsage(r, pkg) }, remainingMs)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking total usage: ${e.message}")
        }
    }

    override fun onInterrupt() {}

    companion object {
        private const val TAG = "AppTrackerService"
        var lastEventTime: Long = 0
            private set
    }
}
