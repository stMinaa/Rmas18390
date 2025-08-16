package com.example.proba.service

import android.content.Context
import android.content.SharedPreferences

object ServiceStateManager {
    private const val PREFS_NAME = "tracking_prefs"
    private const val KEY_IS_SERVICE_RUNNING = "is_service_running"

    fun saveServiceState(context: Context, isRunning: Boolean) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_SERVICE_RUNNING, isRunning).apply()
    }

    fun isServiceRunning(context: Context): Boolean {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_SERVICE_RUNNING, false)
    }
}
