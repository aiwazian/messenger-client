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

/**
 * Возвращает сокращённую дату без времени (например, "24 авг.") или null,
 * если это сегодняшний день.
 *
 * Сегодняшнее отдаётся отдельным случаем, а не готовой строкой: спискам нужно
 * решать, показывать ли дату вообще, а сравнивать для этого отформатированный
 * текст было бы гаданием.
 *
 * Слово «в» не добавляется здесь: связка даты со временем у каждого языка своя
 * и живёт в ресурсах.
 *
 * @param now момент, относительно которого решается, сегодня ли это (по умолчанию текущее время).
 * @param zoneId часовой пояс, в котором сравниваются даты (по умолчанию системный).
 */
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

/**
 * Возвращает краткое представление времени последнего сообщения для списка чатов:
 * - сегодня — время "часы:минуты" (например, "12:21");
 * - в течение последней недели (вчера, позавчера и т.д.) — сокращённый день недели
 *   (например, "ПН");
 * - раньше недели, но не больше года назад — день и месяц (например, "20 июл.");
 * - больше года назад — дата "дд.мм.гг" (например, "08.04.25").
 *
 * @param now момент, относительно которого вычисляется формат (по умолчанию текущее время).
 * @param zoneId часовой пояс, в котором отображается время (по умолчанию системный).
 */
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
