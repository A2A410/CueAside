package foz.cueaside.aa;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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
    public String getSettings() {
        return routineManager.getSettings();
    }

    @JavascriptInterface
    public void saveSettings(String json) {
        routineManager.saveSettings(json);
    }

    @JavascriptInterface
    public String getApps() {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<Routine.AppInfo> appInfos = new ArrayList<>();

        for (ApplicationInfo app : apps) {
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0 && (app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                continue;
            }
            Routine.AppInfo info = new Routine.AppInfo();
            info.name = pm.getApplicationLabel(app).toString();
            info.pkg = app.packageName;
            info.icon = getBase64Icon(pm.getApplicationIcon(app));
            appInfos.add(info);
        }
        return gson.toJson(appInfos);
    }

    @JavascriptInterface
    public void requestUsageAccess() {
        context.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @JavascriptInterface
    public void requestNotificationPermission() {
        // Handled in MainActivity for Android 13+
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

    private void refreshRoutines() {
        webView.post(() -> webView.evaluateJavascript("window.onRoutinesUpdated('" + getRoutines() + "')", null));
    }

    private void refreshApps() {
        webView.post(() -> webView.evaluateJavascript("window.onAppsUpdated('" + getApps() + "')", null));
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
