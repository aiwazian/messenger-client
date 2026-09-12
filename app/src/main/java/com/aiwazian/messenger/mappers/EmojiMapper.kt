package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.database.entity.CustomEmojiEntity
import com.aiwazian.messenger.database.entity.EmojiPackEntity
import com.aiwazian.messenger.domain.CustomEmoji
import com.aiwazian.messenger.domain.EmojiPack
import com.aiwazian.messenger.network.dto.CustomEmojiDto
import com.aiwazian.messenger.network.dto.CustomEmojiItemDto
import com.aiwazian.messenger.network.dto.EmojiPackDto

private const val EMOJI_SYMBOL_SEPARATOR = ","

fun CustomEmojiDto.toDomain(): CustomEmoji = CustomEmoji(
    id = id.toLongOrNull() ?: 0L,
    fileId = fileId,
    url = url,
    sortOrder = sortOrder,
    emojis = emojis
)

fun CustomEmojiItemDto.toDomain(): CustomEmoji = CustomEmoji(
    id = id.toLongOrNull() ?: 0L,
    fileId = fileId,
    url = url,
    sortOrder = sortOrder,
    emojis = emojis
)

fun EmojiPackDto.toDomain(): EmojiPack = EmojiPack(
    id = id.toLongOrNull() ?: 0L,
    name = name,
    username = username,
    ownerId = ownerId.toLongOrNull() ?: 0L,
    emojiCount = emojiCount,
    isOwned = isOwned,
    isInstalled = isInstalled,
    emojis = emojis.map { it.toDomain() },
    coverFileId = coverFileId,
    coverUrl = coverUrl
)

fun CustomEmojiEntity.toDomain(): CustomEmoji = CustomEmoji(
    id = id,
    fileId = fileId,
    url = url,
    sortOrder = sortOrder,
    emojis = emojis.split(EMOJI_SYMBOL_SEPARATOR).filter { it.isNotBlank() }
)

fun EmojiPackEntity.toDomain(emojis: List<CustomEmoji>): EmojiPack = EmojiPack(
    id = id,
    name = name,
    username = username,
    ownerId = ownerId,
    emojiCount = emojiCount,
    isOwned = isOwned,
    isInstalled = isInstalled,
    emojis = emojis
)

fun CustomEmoji.toEntity(packId: Long): CustomEmojiEntity = CustomEmojiEntity(
    id = id,
    packId = packId,
    fileId = fileId,
    url = url,
    sortOrder = sortOrder,
    emojis = emojis.joinToString(EMOJI_SYMBOL_SEPARATOR)
)

fun CustomEmojiItemDto.toEntity(): CustomEmojiEntity = CustomEmojiEntity(
    id = id.toLongOrNull() ?: 0L,
    packId = packId.toLongOrNull() ?: 0L,
    fileId = fileId,
    url = url,
    sortOrder = sortOrder,
    emojis = emojis.joinToString(EMOJI_SYMBOL_SEPARATOR)
)

fun EmojiPack.toEntity(sortOrder: Int = 0): EmojiPackEntity = EmojiPackEntity(
    id = id,
    name = name,
    username = username,
    ownerId = ownerId,
    emojiCount = if (emojiCount > 0) emojiCount else emojis.size,
    isOwned = isOwned,
    isInstalled = isInstalled,
    sortOrder = sortOrder
)
