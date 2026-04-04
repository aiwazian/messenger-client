/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.enums

enum class AppLanguage(val code: String, val nativeName: String, val displayName: String) {
    BE("be", "Беларускі", "Belarusian"),
    DE("de", "Deutsch", "German"),
    EN("en", "English", "English"),
    ES("es", "Español", "Spanish"),
    FR("fr", "Français", "French"),
    HY("hy", "Հայերեն", "Armenian"),
    IT("it", "Italian", "Italiano"),
    KK("kk", "Қазақстан", "Kazakh"),
    KO("ko", "한국어", "Korean"),
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
