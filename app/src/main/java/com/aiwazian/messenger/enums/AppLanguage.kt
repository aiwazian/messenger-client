/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.enums

enum class AppLanguage(val code: String, val nativeName: String, val displayName: String) {
    EN("en", "English", "English"),
    ES("es", "Español", "Spanish"),
    FR("fr", "Français", "French"),
    IT("it", "Italian", "Italiano"),
    RU("ru", "Русский", "Russian");
    
    companion object {
        fun fromString(value: String): AppLanguage {
            return entries.firstOrNull {
                it.name.equals(
                    other = value,
                    ignoreCase = true
                )
            } ?: EN
        }
    }
}
