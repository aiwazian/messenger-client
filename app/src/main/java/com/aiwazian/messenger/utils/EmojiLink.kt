package com.aiwazian.messenger.utils

object EmojiLink {
    private const val BASE = "https://aiwazian.ru/addemoji/"
    
    fun build(username: String): String = BASE + username
    
    fun parseUsername(text: String): String? =
        RegexPatterns.EMOJI_LINK.find(text)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
}
