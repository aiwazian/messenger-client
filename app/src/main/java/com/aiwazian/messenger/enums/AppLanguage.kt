/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.enums

enum class AppLanguage(val code: String, val nativeName: String, val displayName: String) {
    RU(
        "ru",
        "Русский",
        "Russian"
    ),
    De(
        "de",
        "Deutsch",
        "German"
    ),
    EN(
        "en",
        "English",
        "English"
    );
    
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
