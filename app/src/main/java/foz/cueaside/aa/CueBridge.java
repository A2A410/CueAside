package foz.cueaside.aa;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class CueBridge {
    private Context context;
    private RoutineManager routineManager;
    private WebView webView;
    private Gson gson;

    public CueBridge(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
        this.routineManager = new RoutineManager(context);
        this.gson = new Gson();
    }

    @JavascriptInterface
    public String getRoutines() {
        return gson.toJson(routineManager.getRoutines());
    }

    @JavascriptInterface
    public void saveRoutine(String json) {
        Routine r = gson.fromJson(json, Routine.class);
        routineManager.addRoutine(r);
        refreshRoutines();
    }

    @JavascriptInterface
    public void deleteRoutine(String id) {
        routineManager.deleteRoutine(id);
        refreshRoutines();
    }

    @JavascriptInterface
    public void toggleRoutine(String id, boolean enabled) {
        routineManager.toggleRoutine(id, enabled);
        refreshRoutines();
    }

    @JavascriptInterface
    public void clearAllData() {
        routineManager.saveRoutines(new ArrayList<>());
        routineManager.saveSettings("{}");
        refreshRoutines();
    }

    @JavascriptInterface
    public String getSettings() {
        return routineManager.getSettings();
    }

    @JavascriptInterface
    public void saveSettings(String json) {
        routineManager.saveSettings(json);
    }

    @JavascriptInterface
    public String getApps() {
        List<Routine.AppInfo> appInfos = new ArrayList<>();
        try {
            PackageManager pm = context.getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

            for (ApplicationInfo app : apps) {
                try {
                    // Filter system apps but keep system apps that have been updated
                    if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0 && (app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                        continue;
                    }
                    Routine.AppInfo info = new Routine.AppInfo();
                    info.name = pm.getApplicationLabel(app).toString();
                    info.pkg = app.packageName;
                    info.icon = getBase64Icon(pm.getApplicationIcon(app));
                    appInfos.add(info);
                } catch (Exception e) {
                    log("Error processing app " + app.packageName + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log("Error getting apps: " + e.getMessage());
        }
        return gson.toJson(appInfos);
    }

    @JavascriptInterface
    public String checkPermissionsStatus() {
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        status.put("usage", isUsageStatsEnabled());

        boolean accEnabled = isAccessibilityEnabled();
        status.put("accessibility", accEnabled);
        status.put("isServiceRunning", AppTrackerService.isRunning());

        long lastEvent = context.getSharedPreferences("CueAsidePrefs", Context.MODE_PRIVATE)
                                .getLong("last_acc_event", 0);
        status.put("lastAccEvent", Math.max(lastEvent, AppTrackerService.getLastEventTime()));

        status.put("notifications", isNotificationPermissionGranted());
        status.put("hasActiveRoutines", routineManager.getRoutines().stream().anyMatch(r -> r.enabled));
        return gson.toJson(status);
    }

    private boolean isUsageStatsEnabled() {
        try {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAccessibilityEnabled() {
        try {
            int accessibilityEnabled = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
            if (accessibilityEnabled == 1) {
                String settingValue = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                if (settingValue != null) {
                    String[] services = settingValue.split(":");
                    String myService = context.getPackageName() + "/" + AppTrackerService.class.getName();
                    for (String service : services) {
                        if (service.equalsIgnoreCase(myService)) return true;
                    }
                }
            }
        } catch (Exception e) {
            log("Error checking accessibility: " + e.getMessage());
        }
        return false;
    }

    private boolean isNotificationPermissionGranted() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @JavascriptInterface
    public void requestUsageAccess() {
        try {
            context.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            log("Error requesting usage access: " + e.getMessage());
        }
    }

    @JavascriptInterface
    public void requestAccessibilitySettings() {
        try {
            context.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            log("Error requesting accessibility settings: " + e.getMessage());
        }
    }

    @JavascriptInterface
    public void requestNotificationPermission() {
        try {
            Intent intent = new Intent();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
            } else {
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("app_package", context.getPackageName());
                intent.putExtra("app_uid", context.getApplicationInfo().uid);
            }
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            log("Error requesting notification settings: " + e.getMessage());
        }
    }

    @JavascriptInterface
    public void requestBatteryIgnore() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    @JavascriptInterface
    public void requestBootStart() {
        // Instruction for the user
    }

    @JavascriptInterface
    public void rescanApps() {
        refreshApps();
    }

    @JavascriptInterface
    public void log(String message) {
        android.util.Log.d("CueAsideBridge", message);
    }

    @JavascriptInterface
    public void openAppInfo() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            log("Error opening app info: " + e.getMessage());
        }
    }

    private void refreshRoutines() {
        webView.post(() -> {
            String json = getRoutines();
            webView.evaluateJavascript(String.format("window.onRoutinesUpdated(%s)", gson.toJson(json)), null);
        });
    }

    public void refreshApps() {
        new Thread(() -> {
            String json = getApps();
            webView.post(() -> {
                webView.evaluateJavascript(String.format("window.onAppsUpdated(%s)", gson.toJson(json)), null);
            });
        }).start();
    }

    private String getBase64Icon(Drawable drawable) {
        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        return "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }
}
