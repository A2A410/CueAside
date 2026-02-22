package foz.cueaside.aa;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {
    private static final String CHANNEL_ID_DEFAULT = "cue_aside_notifications";
    private static final String CHANNEL_ID_HIGH = "cue_aside_notifications_high";
    private static final Handler handler = new Handler(Looper.getMainLooper());

    public static void showNotification(Context context, Routine routine) {
        try {
            String title = routine.title;
            String message = routine.msg;
            boolean highPriority = routine.highPriority;
            int timeout = routine.timeout;

            String channelId = highPriority ? CHANNEL_ID_HIGH : CHANNEL_ID_DEFAULT;
            createNotificationChannel(context, channelId, highPriority);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title != null && !title.isEmpty() ? title : "CueAside")
                    .setContentText(message)
                    .setPriority(highPriority ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            // Handle Icon
            if (routine.icon != null && "preset".equals(routine.icon.type)) {
                // In a real app we might convert emoji to bitmap, but for now we use default icon
                // and maybe put emoji in title if title is empty
            }

            final int notificationId = (int) System.currentTimeMillis();
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                notificationManager.notify(notificationId, builder.build());

                if (timeout > 0) {
                    handler.postDelayed(() -> {
                        notificationManager.cancel(notificationId);
                    }, timeout * 1000L);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("NotificationHelper", "Error showing notification: " + e.getMessage());
        }
    }

    private static void createNotificationChannel(Context context, String channelId, boolean high) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = high ? "CueAside High Priority" : "CueAside Notifications";
            String description = high ? "High priority alerts" : "Standard alerts";
            int importance = high ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            if (high) {
                channel.enableLights(true);
                channel.enableVibration(true);
                channel.setBypassDnd(true);
            }

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
