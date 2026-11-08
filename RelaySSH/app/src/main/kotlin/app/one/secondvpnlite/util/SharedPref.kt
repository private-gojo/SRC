package app.one.secondvpnlite.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Kotlin implementation of the original SharedPref API.
 * Java-callable method names are preserved for the remaining legacy code.
 */
class SharedPref private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    fun putString(key: String, value: String) {
        editor.putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String): String? =
        sharedPreferences.getString(key, defaultValue)

    fun putInt(key: String, value: Int) {
        editor.putInt(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int): Int =
        sharedPreferences.getInt(key, defaultValue)

    fun putBoolean(key: String, value: Boolean) {
        editor.putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        sharedPreferences.getBoolean(key, defaultValue)

    fun putFloat(key: String, value: Float) {
        editor.putFloat(key, value).apply()
    }

    fun getFloat(key: String, defaultValue: Float): Float =
        sharedPreferences.getFloat(key, defaultValue)

    fun putLong(key: String, value: Long) {
        editor.putLong(key, value).apply()
    }

    fun getLong(key: String, defaultValue: Long): Long =
        sharedPreferences.getLong(key, defaultValue)

    fun putDouble(key: String, value: Double) {
        editor.putString(key, java.lang.Double.toString(value)).apply()
    }

    fun getDouble(key: String, defaultValue: Double): Double {
        val value = sharedPreferences.getString(key, null) ?: return defaultValue
        return try {
            java.lang.Double.parseDouble(value)
        } catch (error: NumberFormatException) {
            error.printStackTrace()
            defaultValue
        }
    }

    fun putStringSet(key: String, value: MutableSet<String>) {
        editor.putStringSet(key, value).apply()
    }

    fun getStringSet(key: String, defaultValue: MutableSet<String>): MutableSet<String>? =
        sharedPreferences.getStringSet(key, defaultValue)

    fun remove(key: String) {
        editor.remove(key).apply()
    }

    fun remove(key: String, defaultValue: String) {
        editor.remove(key + defaultValue).apply()
    }

    fun clear() {
        editor.clear().apply()
    }

    fun removeAppFromList(appToRemove: String) {
        val appList = HashSet(sharedPreferences.getStringSet(KEY_SELECTED_APPS, HashSet()) ?: emptySet())
        appList.remove(appToRemove)
        editor.putStringSet(KEY_SELECTED_APPS, appList).apply()
    }

    companion object {
        private const val PREF_NAME = "SECONDVPN_PREFS"

        @JvmField
        val KEY_SELECTED_APPS: String = "selectedApps"

        @Volatile
        private var instance: SharedPref? = null

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): SharedPref {
            return instance ?: SharedPref(context).also { instance = it }
        }
    }
}
