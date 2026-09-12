package com.aiwazian.messenger.domain

data class CustomEmoji(
    val id: Long,
    val fileId: String,
    val url: String,
    val sortOrder: Int,
    val emojis: List<String> = emptyList()
)

data class CustomEmojiDraft(
    val fileId: String,
    val emojis: List<String>
)

data class EmojiPack(
    val id: Long,
    val name: String,
    val username: String,
    val ownerId: Long,
    val emojiCount: Int,
    val isOwned: Boolean,
    val isInstalled: Boolean,
    val emojis: List<CustomEmoji>,
    val coverFileId: String? = null,
    val coverUrl: String? = null
) {
    val coverEmoji: CustomEmoji?
        get() = emojis.firstOrNull()
    
    val coverImageUrl: String?
        get() = coverUrl ?: coverEmoji?.url
    
    val coverCacheKey: String?
        get() = coverFileId ?: coverEmoji?.fileId
}
