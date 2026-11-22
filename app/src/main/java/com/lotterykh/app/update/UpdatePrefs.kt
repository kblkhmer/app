package com.lotterykh.app.update

import android.content.Context

/** Remembers which Play release we've already alerted the user about, so we don't nag. */
class UpdatePrefs(context: Context) {

    private val prefs = context.getSharedPreferences("app_update", Context.MODE_PRIVATE)

    /** Highest versionCode a system notification has been posted (or the in-app sheet shown) for. */
    var lastNotifiedVersion: Int
        get() = prefs.getInt(KEY_LAST_NOTIFIED, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_NOTIFIED, value).apply()

    var immediateUpdateInProgress: Boolean
        get() = prefs.getBoolean(KEY_IMMEDIATE, false)
        set(value) = prefs.edit().putBoolean(KEY_IMMEDIATE, value).apply()

    var notificationPermissionAsked: Boolean
        get() = prefs.getBoolean(KEY_PERMISSION_ASKED, false)
        set(value) = prefs.edit().putBoolean(KEY_PERMISSION_ASKED, value).apply()

    fun snooze(versionCode: Int) {
        prefs.edit()
            .putInt(KEY_SNOOZED_VERSION, versionCode)
            .putLong(KEY_SNOOZED_AT, System.currentTimeMillis())
            .apply()
    }

    fun isSnoozed(versionCode: Int): Boolean {
        if (prefs.getInt(KEY_SNOOZED_VERSION, 0) != versionCode) return false
        return System.currentTimeMillis() - prefs.getLong(KEY_SNOOZED_AT, 0) < SNOOZE_MS
    }

    companion object {
        private const val KEY_LAST_NOTIFIED = "last_notified_version"
        private const val KEY_IMMEDIATE = "immediate_in_progress"
        private const val KEY_PERMISSION_ASKED = "notification_permission_asked"
        private const val KEY_SNOOZED_VERSION = "snoozed_version"
        private const val KEY_SNOOZED_AT = "snoozed_at"
        private const val SNOOZE_MS = 24L * 60 * 60 * 1000
    }
}
