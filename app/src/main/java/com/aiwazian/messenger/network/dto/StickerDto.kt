/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StickerDto(
    @SerialName("id") val id: String,
    @SerialName("fileId") val fileId: String,
    @SerialName("url") val url: String,
    @SerialName("emojis") val emojis: List<String> = emptyList(),
    @SerialName("sortOrder") val sortOrder: Int = 0
)

@Serializable
data class StickerPackDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("username") val username: String,
    @SerialName("ownerId") val ownerId: String,
    @SerialName("stickerCount") val stickerCount: Int = 0,
    @SerialName("isOwned") val isOwned: Boolean = false,
    @SerialName("isInstalled") val isInstalled: Boolean = false,
    @SerialName("stickers") val stickers: List<StickerDto> = emptyList()
)

@Serializable
data class StickerInputDto(
    @SerialName("fileId") val fileId: String,
    @SerialName("emojis") val emojis: List<String>
)

@Serializable
data class CreateStickerPackRequestDto(
    @SerialName("name") val name: String,
    @SerialName("username") val username: String,
    @SerialName("stickers") val stickers: List<StickerInputDto>
)

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

@Serializable
data class StickerFileDto(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("size") val size: Long = 0,
    @SerialName("mimeType") val mimeType: String = ""
)
