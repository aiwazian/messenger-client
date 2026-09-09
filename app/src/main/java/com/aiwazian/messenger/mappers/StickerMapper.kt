package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.database.entity.StickerEntity
import com.aiwazian.messenger.database.entity.StickerPackEntity
import com.aiwazian.messenger.domain.Sticker
import com.aiwazian.messenger.domain.StickerPack
import com.aiwazian.messenger.network.dto.StickerDto
import com.aiwazian.messenger.network.dto.StickerPackDto

private const val EMOJI_SEPARATOR = ","

fun StickerDto.toDomain(): Sticker = Sticker(
    id = id.toLongOrNull() ?: 0L,
    fileId = fileId,
    url = url,
    sortOrder = sortOrder,
    emojis = emojis
)

fun StickerPackDto.toDomain(): StickerPack = StickerPack(
    id = id.toLongOrNull() ?: 0L,
    name = name,
    username = username,
    ownerId = ownerId.toLongOrNull() ?: 0L,
    stickerCount = stickerCount,
    isOwned = isOwned,
    isInstalled = isInstalled,
    stickers = stickers.map { it.toDomain() },
    coverFileId = coverFileId,
    coverUrl = coverUrl
)

fun StickerEntity.toDomain(): Sticker = Sticker(
    id = id,
    fileId = fileId,
    url = url,
    sortOrder = sortOrder,
    emojis = emojis.split(EMOJI_SEPARATOR).filter { it.isNotBlank() }
)

fun StickerPackEntity.toDomain(stickers: List<Sticker>): StickerPack = StickerPack(
    id = id,
    name = name,
    username = username,
    ownerId = ownerId,
    stickerCount = stickerCount,
    isOwned = isOwned,
    isInstalled = isInstalled,
    stickers = stickers
)

fun Sticker.toEntity(packId: Long): StickerEntity = StickerEntity(
    id = id,
    packId = packId,
    fileId = fileId,
    url = url,
    sortOrder = sortOrder,
    emojis = emojis.joinToString(EMOJI_SEPARATOR)
)

fun StickerPack.toEntity(sortOrder: Int = 0): StickerPackEntity = StickerPackEntity(
    id = id,
    name = name,
    username = username,
    ownerId = ownerId,
    stickerCount = if (stickerCount > 0) stickerCount else stickers.size,
    isOwned = isOwned,
    isInstalled = isInstalled,
    sortOrder = sortOrder
)
