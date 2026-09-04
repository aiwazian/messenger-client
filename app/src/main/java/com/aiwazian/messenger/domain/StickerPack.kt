/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

/**
 * Стикер в наборе.
 *
 * [fileId] держится рядом с [url] ключом кеша: адрес меняется вместе с CDN, а
 * сама картинка — нет.
 */
data class Sticker(
    val id: Long,
    val fileId: String,
    val url: String,
    val sortOrder: Int
)

/**
 * Набор стикеров.
 *
 * [stickers] пуст в списках наборов: там нужно только [stickerCount].
 */
data class StickerPack(
    val id: Long,
    val name: String,
    val username: String,
    val ownerId: Long,
    val stickerCount: Int,
    /** Набор создан текущим пользователем: только ему доступно редактирование. */
    val isOwned: Boolean,
    val isInstalled: Boolean,
    val stickers: List<Sticker>
) {
    /** Картинка для карточки набора — первый стикер, если состав уже загружен. */
    val coverSticker: Sticker?
        get() = stickers.firstOrNull()
}
