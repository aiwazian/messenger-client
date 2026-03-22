/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.extensions

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Возвращает строковое представление времени в формате "часы:минуты" (например, "14:42").
 */
fun Instant.toPrettyTime(): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
        .withLocale(Locale.getDefault())
    return this.atZone(ZoneId.systemDefault()).format(formatter)
}

/**
 * Возвращает дату в формате "день месяц" (например, "15 января").
 */
fun Instant.toPrettyDate(): String {
    val formatter = DateTimeFormatter.ofPattern("d MMMM")
        .withZone(ZoneId.systemDefault())
        .withLocale(Locale.getDefault())
    return this.atZone(ZoneId.systemDefault()).format(formatter)
}

/**
 * Возвращает дату в формате "день месяц год" (например, "15 января 2024").
 */
fun Instant.toPrettyDateWithYear(): String {
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
        .withZone(ZoneId.systemDefault())
        .withLocale(Locale.getDefault())
    return this.atZone(ZoneId.systemDefault()).format(formatter)
}

/**
 * Возвращает полный формат "день месяц год часы:минуты" (например, "15 января 2024 14:42").
 */
fun Instant.toPrettyDateTime(): String {
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
        .withLocale(Locale.getDefault())
    return this.atZone(ZoneId.systemDefault()).format(formatter)
}
