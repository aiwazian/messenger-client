package com.aiwazian.messenger.utils

import com.aiwazian.messenger.R
import com.aiwazian.messenger.extensions.toPrettyTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object LastSeenHelper {
    fun getSubtitle(isOnline: Boolean, lastSeen: Long?): UiText {
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
            today -> UiText.StringResource(R.string.last_seen_time, time)
            
            today.minusDays(1) -> UiText.StringResource(R.string.last_seen_yesterday_time, time)
            
            else -> UiText.StringResource(
                R.string.last_seen_date_time,
                formatShortDate(instant, zone, isCurrentYear = date.year == today.year),
                time
            )
        }
    }
    
    private fun formatShortDate(instant: Instant, zone: ZoneId, isCurrentYear: Boolean): String {
        val pattern = if (isCurrentYear) "d MMM" else "d MMM yyyy"
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            .withZone(zone)
        
        return formatter.format(instant)
    }
}
