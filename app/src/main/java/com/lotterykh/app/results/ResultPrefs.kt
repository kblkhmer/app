package com.lotterykh.app.results

import android.content.Context

/** Tracks which draws the user has already been alerted about, plus the on/off switch. */
class ResultPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("result_alerts", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /** API date ("yyyy-MM-dd") that [seenDraws] belongs to; null before the first check. */
    val seenDate: String?
        get() = prefs.getString(KEY_SEEN_DATE, null)

    val seenDraws: Set<String>
        get() = prefs.getStringSet(KEY_SEEN_DRAWS, emptySet()) ?: emptySet()

    fun saveSeen(date: String, draws: Set<String>) {
        prefs.edit()
            .putString(KEY_SEEN_DATE, date)
            .putStringSet(KEY_SEEN_DRAWS, HashSet(draws))
            .apply()
    }

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_SEEN_DATE = "seen_date"
        private const val KEY_SEEN_DRAWS = "seen_draws"
    }
}
