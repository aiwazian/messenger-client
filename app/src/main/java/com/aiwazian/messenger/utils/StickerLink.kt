package com.aiwazian.messenger.utils

object StickerLink {
    private const val BASE = "https://aiwazian.ru/addstickers/"
    
    fun build(username: String): String = BASE + username
    
    fun parseUsername(text: String): String? =
        RegexPatterns.STICKER_LINK.find(text)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
}
