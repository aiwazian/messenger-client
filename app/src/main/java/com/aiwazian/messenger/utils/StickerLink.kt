package com.aiwazian.messenger.utils

object StickerLink {
    private const val BASE = "https://aiwazian.ru/addstickers/"
    
    private val PATTERN = Regex("(?:https?://)?(?:www\\.)?aiwazian\\.ru/addstickers/([A-Za-z0-9_]+)")
    
    fun build(username: String): String = BASE + username
    
    fun parseUsername(text: String): String? =
        PATTERN.find(text)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
}
