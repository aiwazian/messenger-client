package com.aiwazian.messenger.enums

enum class ThemeOption {
    LIGHT,
    DARK,
    SYSTEM;
    
    companion object {
        fun fromString(value: String): ThemeOption {
            return entries.firstOrNull {
                it.name.equals(
                    value,
                    ignoreCase = true
                )
            } ?: SYSTEM
        }
    }
}