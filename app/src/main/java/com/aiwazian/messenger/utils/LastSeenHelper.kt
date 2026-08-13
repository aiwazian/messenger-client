/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import android.content.Context
import com.aiwazian.messenger.R
import com.aiwazian.messenger.extensions.toPrettyTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object LastSeenHelper {
    /**
     * Подпись о последнем заходе.
     *
     * Точное время показывается всегда, когда сервер его прислал: и через минуту
     * после выхода, и через месяц. Раньше первые пять минут подменялись
     * расплывчатым «в сети недавно», из-за чего свежее время терялось.
     *
     * «В сети недавно» осталось только признаком скрытого статуса: при
     * приватности NOBODY сервер не отдаёт lastSeen вообще, поэтому сюда
     * приходит null.
     */
    fun getSubtitle(context: Context, isOnline: Boolean, lastSeen: Long?): UiText {
        if (isOnline) {
            return UiText.StringResource(R.string.online)
        }
        
        if (lastSeen == null) {
            return UiText.StringResource(R.string.last_seen_recently)
        }
        
        val zone = ZoneId.systemDefault()
        val instant = Instant.ofEpochMilli(lastSeen)
        val date = instant.atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        val time = instant.toPrettyTime()
        
        return when (date) {
            today -> UiText.DynamicString(
                context.getString(R.string.last_seen_time, time)
            )
            
            today.minusDays(1) -> UiText.DynamicString(
                context.getString(R.string.last_seen_yesterday_time, time)
            )
            
            else -> UiText.DynamicString(
                context.getString(
                    R.string.last_seen_date_time,
                    formatShortDate(instant, zone, isCurrentYear = date.year == today.year),
                    time
                )
            )
        }
    }
    
    /*
     * Месяц сокращается («3 авг.»), чтобы подпись не растягивала заголовок чата
     * на две строки. Год дописывается только для прошлых лет.
     */
    private fun formatShortDate(instant: Instant, zone: ZoneId, isCurrentYear: Boolean): String {
        val pattern = if (isCurrentYear) "d MMM" else "d MMM yyyy"
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            .withZone(zone)
        
        return formatter.format(instant)
    }
}
