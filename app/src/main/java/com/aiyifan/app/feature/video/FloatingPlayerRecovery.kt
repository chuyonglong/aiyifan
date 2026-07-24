package com.aiyifan.app.feature.video

import android.content.Context

object FloatingPlayerRecovery {
    private const val PREFERENCES_NAME = "floating_player_recovery"
    private const val KEY_PENDING = "pending"
    private const val KEY_POSITION_MS = "position_ms"

    fun record(context: Context, positionMs: Long) {
        preferences(context).edit()
            .putBoolean(KEY_PENDING, true)
            .putLong(KEY_POSITION_MS, positionMs.coerceAtLeast(0L))
            .apply()
    }

    fun consumePosition(context: Context): Long? {
        val preferences = preferences(context)
        if (!preferences.getBoolean(KEY_PENDING, false)) return null

        val positionMs = preferences.getLong(KEY_POSITION_MS, 0L)
        preferences.edit().clear().apply()
        return positionMs
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
