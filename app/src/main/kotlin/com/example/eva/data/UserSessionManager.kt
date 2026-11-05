package com.example.eva.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user profile, Google cloud authentication state, 15 GB storage quotas,
 * and the 14-day username edit restriction for EVA Super-App.
 */
object UserSessionManager {
    private const val PREF_NAME = "eva_user_session"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USERNAME = "username"
    private const val KEY_EMAIL = "email"
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val KEY_LAST_USERNAME_CHANGE = "last_username_change"
    private const val KEY_STORAGE_USED_BYTES = "storage_used_bytes"
    private const val FOURTEEN_DAYS_MILLIS = 14L * 24 * 60 * 60 * 1000L

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isLoggedIn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUsername(context: Context): String {
        return getPrefs(context).getString(KEY_USERNAME, "eva_explorer") ?: "eva_explorer"
    }

    fun getEmail(context: Context): String {
        return getPrefs(context).getString(KEY_EMAIL, "user@gmail.com") ?: "user@gmail.com"
    }

    fun getDisplayName(context: Context): String {
        return getPrefs(context).getString(KEY_DISPLAY_NAME, "EVA Explorer") ?: "EVA Explorer"
    }

    fun getCloudStorageUsedMb(context: Context): Float {
        val bytes = getPrefs(context).getLong(KEY_STORAGE_USED_BYTES, 184L * 1024 * 1024) // Initial seed ~184 MB
        return bytes / (1024f * 1024f)
    }

    fun getCloudStorageTotalGb(): Int = 15

    fun canChangeUsername(context: Context): Boolean {
        val lastChange = getPrefs(context).getLong(KEY_LAST_USERNAME_CHANGE, 0L)
        if (lastChange == 0L) return true
        val elapsed = System.currentTimeMillis() - lastChange
        return elapsed >= FOURTEEN_DAYS_MILLIS
    }

    fun daysRemainingForUsernameChange(context: Context): Long {
        val lastChange = getPrefs(context).getLong(KEY_LAST_USERNAME_CHANGE, 0L)
        if (lastChange == 0L) return 0L
        val elapsed = System.currentTimeMillis() - lastChange
        val remainingMillis = FOURTEEN_DAYS_MILLIS - elapsed
        return if (remainingMillis > 0) {
            val days = remainingMillis / (24 * 60 * 60 * 1000L)
            if (days == 0L) 1L else days
        } else {
            0L
        }
    }

    fun saveSession(
        context: Context,
        username: String,
        email: String,
        displayName: String
    ) {
        val cleanUsername = if (username.startsWith("@")) username else "@$username"
        getPrefs(context).edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USERNAME, cleanUsername)
            .putString(KEY_EMAIL, email)
            .putString(KEY_DISPLAY_NAME, displayName)
            .putLong(KEY_LAST_USERNAME_CHANGE, System.currentTimeMillis())
            .putLong(KEY_STORAGE_USED_BYTES, 184L * 1024 * 1024) // 184 MB default initialization
            .apply()
    }

    fun updateUsername(context: Context, newUsername: String): Boolean {
        if (!canChangeUsername(context)) return false
        val cleanUsername = if (newUsername.startsWith("@")) newUsername else "@$newUsername"
        getPrefs(context).edit()
            .putString(KEY_USERNAME, cleanUsername)
            .putLong(KEY_LAST_USERNAME_CHANGE, System.currentTimeMillis())
            .apply()
        return true
    }

    fun logout(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
