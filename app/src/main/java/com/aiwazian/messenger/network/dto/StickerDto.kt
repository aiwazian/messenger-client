/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Один стикер в наборе.
 *
 * Идентификаторы приходят строками: на сервере это BigInt, а в JSON такое
 * число не влезает без потерь.
 */
@Serializable
data class StickerDto(
    @SerialName("id") val id: String,
    /**
     * Идентификатор файла в хранилище — ключ кеша картинки.
     *
     * Кешировать по [url] нельзя: домен меняется вместе с CDN, а картинка
     * остаётся той же — иначе весь набор качался бы заново.
     */
    @SerialName("fileId") val fileId: String,
    /** Постоянная ссылка: без подписи и без срока жизни. */
    @SerialName("url") val url: String,
    @SerialName("sortOrder") val sortOrder: Int = 0
)

/**
 * Набор стикеров.
 *
 * [stickers] пуст в ответах со списками наборов — там рисуется только карточка,
 * а количество берётся из [stickerCount].
 */
@Serializable
data class StickerPackDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("username") val username: String,
    @SerialName("ownerId") val ownerId: String,
    @SerialName("stickerCount") val stickerCount: Int = 0,
    /** Набор создан текущим пользователем: только ему доступно редактирование. */
    @SerialName("isOwned") val isOwned: Boolean = false,
    @SerialName("isInstalled") val isInstalled: Boolean = false,
    @SerialName("stickers") val stickers: List<StickerDto> = emptyList()
)

/**
 * Стикер в запросе на создание или изменение набора.
 *
 * Картинка здесь не передаётся: к этому моменту файл уже лежит в хранилище.
 * Порядок задаёт порядок в массиве, поэтому номер не шлётся.
 */
@Serializable
data class StickerInputDto(
    @SerialName("fileId") val fileId: String
)

@Serializable
data class CreateStickerPackRequestDto(
    @SerialName("name") val name: String,
    @SerialName("username") val username: String,
    @SerialName("stickers") val stickers: List<StickerInputDto>
)

/**
 * Изменение набора: переданные поля меняются, остальные остаются как были.
 *
 * Состав шлётся целиком, а не добавлением по одному: повторное нажатие
 * сохранения после обрыва связи не продублирует стикеры.
 */
@Serializable
data class UpdateStickerPackRequestDto(
    @SerialName("name") val name: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("stickers") val stickers: List<StickerInputDto>? = null
)

@Serializable
data class StickerPackUsernameAvailabilityDto(
    @SerialName("available") val available: Boolean = false
)

/**
 * Ответ на подтверждение загрузки стикера.
 *
 * Все поля с значениями по умолчанию: клиенту нужен сам факт успеха, а
 * разбор тела не должен ломаться из-за полей, которые здесь не читаются.
 */
@Serializable
data class StickerFileDto(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("size") val size: Long = 0,
    @SerialName("mimeType") val mimeType: String = ""
)
