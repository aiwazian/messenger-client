/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.domain.Sticker
import com.aiwazian.messenger.domain.StickerPack
import com.aiwazian.messenger.network.dto.StickerDto
import com.aiwazian.messenger.network.dto.StickerPackDto

/**
 * Идентификаторы приходят строками и разбираются в Long здесь, а не в экранах.
 *
 * Неразборчивое значение даёт ноль: такого набора не существует, и запросы по
 * нему вернутся ошибкой вместо падения разбора ответа.
 */
fun StickerDto.toDomain(): Sticker = Sticker(
    id = id.toLongOrNull() ?: 0L,
    fileId = fileId,
    url = url,
    sortOrder = sortOrder
)

fun StickerPackDto.toDomain(): StickerPack = StickerPack(
    id = id.toLongOrNull() ?: 0L,
    name = name,
    username = username,
    ownerId = ownerId.toLongOrNull() ?: 0L,
    stickerCount = stickerCount,
    isOwned = isOwned,
    isInstalled = isInstalled,
    stickers = stickers.map { it.toDomain() }
)
