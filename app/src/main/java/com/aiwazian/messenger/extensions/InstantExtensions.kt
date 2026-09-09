/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.extensions

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun Instant.toPrettyTime(): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
        .withLocale(Locale.getDefault())
    return this.atZone(ZoneId.systemDefault()).format(formatter)
}

fun Instant.toPrettyDateWithYear(): String {
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
        .withZone(ZoneId.systemDefault())
        .withLocale(Locale.getDefault())
    return this.atZone(ZoneId.systemDefault()).format(formatter)
}

fun Instant.toPrettyDateTime(): String {
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
        .withLocale(Locale.getDefault())
    return this.atZone(ZoneId.systemDefault()).format(formatter)
}

fun Instant.toShortDateIfNotToday(
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): String? {
    val dateTime = this.atZone(zoneId)
    val today = now.atZone(zoneId).toLocalDate()
    
    if (!dateTime.toLocalDate().isBefore(today)) {
        return null
    }
    
    return dateTime.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
}

fun Instant.toChatListTime(
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    val locale = Locale.getDefault()
    val messageDateTime = this.atZone(zoneId)
    val messageDate = messageDateTime.toLocalDate()
    val today = now.atZone(zoneId).toLocalDate()

    val pattern = when {
        !messageDate.isBefore(today) -> "HH:mm"
        messageDate.isAfter(today.minusDays(7)) -> "EEE"
        messageDate.isAfter(today.minusYears(1)) -> "d MMM"
        else -> "dd.MM.yy"
    }

    val formatted = messageDateTime.format(DateTimeFormatter.ofPattern(pattern, locale))

    return if (pattern == "EEE") formatted.uppercase(locale) else formatted
}
