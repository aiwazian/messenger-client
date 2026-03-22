/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.enums

enum class AppLanguage(val code: String, val title: String) {
    RU("ru", "Русский"),
    EN("en", "English");
    
    companion object {
        fun fromString(value: String): AppLanguage {
            return entries.firstOrNull {
                it.name.equals(
                    value,
                    ignoreCase = true
                )
            } ?: EN
        }
    }
}
