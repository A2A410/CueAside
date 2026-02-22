package foz.cueaside.aa;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RoutineManager {
    private static final String PREF_NAME = "CueAsidePrefs";
    private static final String KEY_ROUTINES = "routines";
    private static final String KEY_SETTINGS = "settings";

    private SharedPreferences prefs;
    private Gson gson;

    public RoutineManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public List<Routine> getRoutines() {
        String json = prefs.getString(KEY_ROUTINES, "[]");
        Type type = new TypeToken<ArrayList<Routine>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public void saveRoutines(List<Routine> routines) {
        String json = gson.toJson(routines);
        prefs.edit().putString(KEY_ROUTINES, json).apply();
    }

    public void addRoutine(Routine routine) {
        List<Routine> routines = getRoutines();
        routines.add(0, routine);
        saveRoutines(routines);
    }

    public void deleteRoutine(String id) {
        List<Routine> routines = getRoutines();
        routines.removeIf(r -> r.id.equals(id));
        saveRoutines(routines);
    }

    public void toggleRoutine(String id, boolean enabled) {
        List<Routine> routines = getRoutines();
        for (Routine r : routines) {
            if (r.id.equals(id)) {
                r.enabled = enabled;
                break;
            }
        }
        saveRoutines(routines);
    }

    public String getSettings() {
        return prefs.getString(KEY_SETTINGS, "{}");
    }

    public void saveSettings(String settingsJson) {
        prefs.edit().putString(KEY_SETTINGS, settingsJson).apply();
    }
}
