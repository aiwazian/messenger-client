/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.enums

import androidx.compose.ui.graphics.Color

enum class AppPrimaryColor(val color: Color) {
    Blue(Color(0xFF60A8E7)),
    Green(Color(0xFF4E9C57)),
    Lime(Color(0xFF3FC1B0)),
    Pink(Color(0xFFCA7896)),
    Purple(Color(0xFFA58ED2)),
    Orange(Color(0xFFFF5722)),
    Coral(Color(0xFFD27570)),
    Gray(Color(0xFF7B8799)),
    Yellow(Color(0xFFCBAC67));
    
    companion object {
        fun fromString(value: String): AppPrimaryColor {
            return entries.firstOrNull {
                it.name.equals(
                    value,
                    ignoreCase = true
                )
            } ?: Blue
        }
    }
}
