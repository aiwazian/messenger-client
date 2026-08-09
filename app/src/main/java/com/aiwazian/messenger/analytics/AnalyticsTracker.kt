/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Tracks app usage in Firebase Analytics.
 *
 * Firebase automatically calculates users, sessions and engagement time. The
 * custom events below make per-user session count, exact session duration and
 * total online time easier to inspect in GA4/Firebase reports.
 */
object AnalyticsTracker {
    private const val EVENT_APP_SESSION_START = "app_session_start"
    private const val EVENT_APP_SESSION_END = "app_session_end"
    private const val EVENT_AUTH_SCREEN_OPEN = "auth_screen_open"
    private const val EVENT_MAIN_SCREEN_OPEN = "main_screen_open"

    private const val PARAM_SESSION_ID = "session_id"
    private const val PARAM_SESSION_NUMBER = "session_number"
    private const val PARAM_SESSION_DURATION_MS = "session_duration_ms"
    private const val PARAM_SESSION_DURATION_SEC = "session_duration_sec"
    private const val PARAM_TOTAL_ONLINE_TIME_MS = "total_online_time_ms"
    private const val PARAM_TOTAL_ONLINE_TIME_SEC = "total_online_time_sec"
    private const val PARAM_IS_AUTHORIZED = "is_authorized"

    private const val USER_PROPERTY_APP_SESSION_COUNT = "app_session_count"
    private const val USER_PROPERTY_TOTAL_ONLINE_TIME_SEC = "total_online_time_sec"

    private const val PREFS_NAME = "analytics_session_prefs"
    private const val KEY_SESSION_COUNT = "session_count"
    private const val KEY_TOTAL_ONLINE_TIME_MS = "total_online_time_ms"

    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var currentSessionId: String? = null
    private var sessionStartedAtMs: Long = 0L
    private var sessionNumber: Long = 0L

    fun init(context: Context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext)
    }

    fun setCurrentUser(userId: Long?) {
        firebaseAnalytics?.setUserId(userId?.toString())
    }

    fun trackMainScreenOpen() {
        firebaseAnalytics?.logEvent(EVENT_MAIN_SCREEN_OPEN, null)
    }

    fun trackAuthScreenOpen() {
        firebaseAnalytics?.logEvent(EVENT_AUTH_SCREEN_OPEN, null)
    }

    @Synchronized
    fun startSession(context: Context, isAuthorized: Boolean) {
        if (currentSessionId != null) {
            return
        }

        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sessionNumber = prefs.getLong(KEY_SESSION_COUNT, 0L) + 1L
        prefs.edit().putLong(KEY_SESSION_COUNT, sessionNumber).apply()

        currentSessionId = UUID.randomUUID().toString()
        sessionStartedAtMs = System.currentTimeMillis()

        firebaseAnalytics?.setUserProperty(USER_PROPERTY_APP_SESSION_COUNT, sessionNumber.toString())
        firebaseAnalytics?.logEvent(
            EVENT_APP_SESSION_START,
            Bundle().apply {
                putString(PARAM_SESSION_ID, currentSessionId)
                putLong(PARAM_SESSION_NUMBER, sessionNumber)
                putLong(PARAM_IS_AUTHORIZED, if (isAuthorized) 1L else 0L)
            }
        )
    }

    @Synchronized
    fun endSession(context: Context) {
        val sessionId = currentSessionId ?: return
        val durationMs = (System.currentTimeMillis() - sessionStartedAtMs).coerceAtLeast(0L)

        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val totalOnlineTimeMs = prefs.getLong(KEY_TOTAL_ONLINE_TIME_MS, 0L) + durationMs
        prefs.edit().putLong(KEY_TOTAL_ONLINE_TIME_MS, totalOnlineTimeMs).apply()

        val durationSec = TimeUnit.MILLISECONDS.toSeconds(durationMs)
        val totalOnlineTimeSec = TimeUnit.MILLISECONDS.toSeconds(totalOnlineTimeMs)

        firebaseAnalytics?.setUserProperty(USER_PROPERTY_TOTAL_ONLINE_TIME_SEC, totalOnlineTimeSec.toString())
        firebaseAnalytics?.logEvent(
            EVENT_APP_SESSION_END,
            Bundle().apply {
                putString(PARAM_SESSION_ID, sessionId)
                putLong(PARAM_SESSION_NUMBER, sessionNumber)
                putLong(PARAM_SESSION_DURATION_MS, durationMs)
                putLong(PARAM_SESSION_DURATION_SEC, durationSec)
                putLong(PARAM_TOTAL_ONLINE_TIME_MS, totalOnlineTimeMs)
                putLong(PARAM_TOTAL_ONLINE_TIME_SEC, totalOnlineTimeSec)
            }
        )

        currentSessionId = null
        sessionStartedAtMs = 0L
        sessionNumber = 0L
    }
}
