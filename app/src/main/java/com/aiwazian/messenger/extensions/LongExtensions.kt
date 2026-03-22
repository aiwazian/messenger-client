/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.extensions

import java.time.Instant

/**
 * Расширение для типа Long, преобразующее значение в объект Instant.
 * Значение рассматривается как количество миллисекунд с начала эпохи.
 */
fun Long.toInstance(): Instant {
    return Instant.ofEpochMilli(this)
}
