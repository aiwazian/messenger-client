/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.extensions

import java.time.Instant
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

/**
 * Расширение для типа Long, преобразующее значение в объект Instant.
 * Значение рассматривается как количество миллисекунд с начала эпохи.
 */
fun Long.toInstance(): Instant {
    return Instant.ofEpochMilli(this)
}

fun Long.formatFileSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
    return String.format(
        Locale.getDefault(),
        "%.1f %s",
        this / 1024.0.pow(digitGroups.toDouble()),
        units[digitGroups]
    )
}
