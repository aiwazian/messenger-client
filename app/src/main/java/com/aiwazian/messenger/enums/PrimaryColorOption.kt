package com.aiwazian.messenger.enums

import androidx.compose.ui.graphics.Color

enum class PrimaryColorOption(val color: Color) {
    Blue(Color(0xFF2196F3)),
    Green(Color(0xFF4CAF50)),
    DarkGreen(Color(0xFF009688)),
    Purple(Color(0xFF9C27B0)),
    Orange(Color(0xFFFF5722)),
    Orange1(Color(0xFFE91E63)),
    Pink(Color(0xFFFF00FF)),
    Pink1(Color(0xFF673AB7));

    companion object {
        fun fromString(value: String): PrimaryColorOption {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Blue
        }
    }
}