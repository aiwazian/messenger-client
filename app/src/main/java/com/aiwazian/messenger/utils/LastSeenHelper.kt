/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import com.aiwazian.messenger.R
import com.aiwazian.messenger.extensions.toPrettyTime
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object LastSeenHelper {
    fun getSubtitle(context: Context, isOnline: Boolean, lastSeen: Long?): UiText {
        if (isOnline) {
            return UiText.StringResource(R.string.online)
        }
        
        if (lastSeen == null) {
            return UiText.StringResource(R.string.last_seen_recently)
        }
        
        val instant = Instant.ofEpochMilli(lastSeen)
        val now = Instant.now()
        val duration = Duration.between(instant, now)
        val zone = ZoneId.systemDefault()
        val date = instant.atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        
        return when {
            duration.toMinutes() < 5 -> UiText.StringResource(R.string.last_seen_recently)
            date == today -> UiText.DynamicString(
                context.getString(R.string.last_seen_at, instant.toPrettyTime())
            )
            
            date == today.minusDays(1) -> {
                val time = instant.toPrettyTime()
                UiText.DynamicString(context.getString(R.string.last_seen_at, "вчера в $time"))
            }
            
            else -> {
                val formatter = DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())
                    .withZone(zone)
                val dateStr = formatter.format(instant)
                val time = instant.toPrettyTime()
                UiText.DynamicString(context.getString(R.string.last_seen_at, "$dateStr в $time"))
            }
        }
    }
}
