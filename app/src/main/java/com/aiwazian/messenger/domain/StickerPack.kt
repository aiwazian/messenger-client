package com.aiwazian.messenger.domain

data class Sticker(
    val id: Long,
    val fileId: String,
    val url: String,
    val sortOrder: Int,
    val emojis: List<String> = emptyList()
)

data class StickerDraft(
    val fileId: String,
    val emojis: List<String>
)

data class StickerPack(
    val id: Long,
    val name: String,
    val username: String,
    val ownerId: Long,
    val stickerCount: Int,
    val isOwned: Boolean,
    val isInstalled: Boolean,
    val stickers: List<Sticker>,
    val coverFileId: String? = null,
    val coverUrl: String? = null
) {
    val coverSticker: Sticker?
        get() = stickers.firstOrNull()
    
    val coverImageUrl: String?
        get() = coverUrl ?: coverSticker?.url
    
    val coverCacheKey: String?
        get() = coverFileId ?: coverSticker?.fileId
}
