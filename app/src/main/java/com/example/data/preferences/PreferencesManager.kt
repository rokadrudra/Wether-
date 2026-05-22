package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)

    // Cached StateFlows for reactive Compose streams
    private val _tempUnit = MutableStateFlow(getTempUnitPref())
    val tempUnit: StateFlow<String> = _tempUnit

    private val _windUnit = MutableStateFlow(getWindUnitPref())
    val windUnit: StateFlow<String> = _windUnit

    private val _notificationsEnabled = MutableStateFlow(getNotificationsPref())
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled

    private val _rainAlertsEnabled = MutableStateFlow(getRainAlertsPref())
    val rainAlertsEnabled: StateFlow<Boolean> = _rainAlertsEnabled

    private fun getTempUnitPref(): String = prefs.getString("temp_unit", "C") ?: "C"
    fun setTempUnit(unit: String) {
        prefs.edit().putString("temp_unit", unit).apply()
        _tempUnit.value = unit
    }

    private fun getWindUnitPref(): String = prefs.getString("wind_unit", "km/h") ?: "km/h"
    fun setWindUnit(unit: String) {
        prefs.edit().putString("wind_unit", unit).apply()
        _windUnit.value = unit
    }

    private fun getNotificationsPref(): Boolean = prefs.getBoolean("notifications_enabled", true)
    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        _notificationsEnabled.value = enabled
    }

    private fun getRainAlertsPref(): Boolean = prefs.getBoolean("rain_alerts_enabled", true)
    fun setRainAlertsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("rain_alerts_enabled", enabled).apply()
        _rainAlertsEnabled.value = enabled
    }
}
