package com.aiwazian.messenger.enums

enum class Language {
    RU,
    EN;
    
    companion object {
        fun fromString(value: String): Language {
            return entries.firstOrNull {
                it.name.equals(
                    value,
                    ignoreCase = true
                )
            } ?: EN
        }
    }
}