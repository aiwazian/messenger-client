/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.utils

import java.util.Locale
import kotlin.math.floor

/** 940 → «940», 1500 → «1,5К», 15600 → «15,6К», 156000 → «156К», 1200000 → «1,2М». */
fun compactCount(count: Int): String = when {
    count < 1000 -> count.toString()
    count < 1_000_000 -> compactScaled(count / 1000f, "К")
    else -> compactScaled(count / 1_000_000f, "М")
}

private fun compactScaled(value: Float, suffix: String): String {
    val text = if (value >= 100f) {
        floor(value).toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
            .removeSuffix(",0")
            .removeSuffix(".0")
    }

    return "$text$suffix"
}
