package foz.cueaside.aa.native_port.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import foz.cueaside.aa.native_port.model.AppSettings
import foz.cueaside.aa.native_port.model.Routine

class RoutineManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getRoutines(): MutableList<Routine> {
        val json = prefs.getString(KEY_ROUTINES, "[]")
        val type = object : TypeToken<ArrayList<Routine>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    fun saveRoutines(routines: List<Routine>) {
        val json = gson.toJson(routines)
        prefs.edit().putString(KEY_ROUTINES, json).apply()
    }

    fun addRoutine(routine: Routine) {
        val routines = getRoutines()
        routines.add(0, routine)
        saveRoutines(routines)
    }

    fun deleteRoutine(id: String) {
        val routines = getRoutines()
        routines.removeAll { it.id == id }
        saveRoutines(routines)
    }

    fun toggleRoutine(id: String, enabled: Boolean) {
        val routines = getRoutines()
        for (i in routines.indices) {
            if (routines[i].id == id) {
                routines[i] = routines[i].copy(enabled = enabled)
                break
            }
        }
        saveRoutines(routines)
    }

    fun getSettings(): AppSettings {
        val json = prefs.getString(KEY_SETTINGS, "{}")
        return try {
            gson.fromJson(json, AppSettings::class.java) ?: AppSettings()
        } catch (e: Exception) {
            AppSettings()
        }
    }

    fun saveSettings(settings: AppSettings) {
        val json = gson.toJson(settings)
        prefs.edit().putString(KEY_SETTINGS, json).apply()
    }

    companion object {
        private const val PREF_NAME = "CueAsidePrefs"
        private const val KEY_ROUTINES = "routines"
        private const val KEY_SETTINGS = "settings"
    }
}
