package foz.cueaside.aa.native_port.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import foz.cueaside.aa.native_port.model.Routine

object NotificationHelper {
    private const val CHANNEL_ID_DEFAULT = "cue_aside_notifications"
    private const val CHANNEL_ID_HIGH = "cue_aside_notifications_high"
    private val handler = Handler(Looper.getMainLooper())

    fun showNotification(context: Context, routine: Routine) {
        try {
            val title = routine.title
            val message = routine.msg
            val highPriority = routine.highPriority
            val timeout = routine.timeout

            val channelId = if (highPriority) CHANNEL_ID_HIGH else CHANNEL_ID_DEFAULT
            createNotificationChannel(context, channelId, highPriority)

            var displayTitle = if (!title.isNullOrEmpty()) title else "CueAside"
            if (routine.cueName.isNotEmpty()) {
                displayTitle = "[${routine.cueName}] $displayTitle"
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(displayTitle)
                .setContentText(message)
                .setPriority(if (highPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            val notificationId = System.currentTimeMillis().toInt()
            val notificationManager = NotificationManagerCompat.from(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(notificationId, builder.build())
                    if (timeout > 0) {
                        handler.postDelayed({
                            notificationManager.cancel(notificationId)
                        }, timeout * 1000L)
                    }
                }
            } else {
                notificationManager.notify(notificationId, builder.build())
                if (timeout > 0) {
                    handler.postDelayed({
                        notificationManager.cancel(notificationId)
                    }, timeout * 1000L)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Error showing notification: ${e.message}")
        }
    }

    private fun createNotificationChannel(context: Context, channelId: String, high: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = if (high) "CueAside High Priority" else "CueAside Notifications"
            val description = if (high) "High priority alerts" else "Standard alerts"
            val importance = if (high) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(channelId, name, importance).apply {
                this.description = description
                if (high) {
                    enableLights(true)
                    enableVibration(true)
                    setBypassDnd(true)
                }
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
