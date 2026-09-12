package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomEmojiDto(
    @SerialName("id") val id: String,
    @SerialName("fileId") val fileId: String,
    @SerialName("url") val url: String,
    @SerialName("emojis") val emojis: List<String> = emptyList(),
    @SerialName("sortOrder") val sortOrder: Int = 0
)

@Serializable
data class CustomEmojiItemDto(
    @SerialName("id") val id: String,
    @SerialName("packId") val packId: String,
    @SerialName("fileId") val fileId: String,
    @SerialName("url") val url: String,
    @SerialName("emojis") val emojis: List<String> = emptyList(),
    @SerialName("sortOrder") val sortOrder: Int = 0
)

@Serializable
data class EmojiPackDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("username") val username: String,
    @SerialName("ownerId") val ownerId: String,
    @SerialName("coverFileId") val coverFileId: String? = null,
    @SerialName("coverUrl") val coverUrl: String? = null,
    @SerialName("emojiCount") val emojiCount: Int = 0,
    @SerialName("isOwned") val isOwned: Boolean = false,
    @SerialName("isInstalled") val isInstalled: Boolean = false,
    @SerialName("emojis") val emojis: List<CustomEmojiDto> = emptyList()
)

@Serializable
data class EmojiInputDto(
    @SerialName("fileId") val fileId: String,
    @SerialName("emojis") val emojis: List<String>
)

@Serializable
data class CreateEmojiPackRequestDto(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String,
    @SerialName("username") val username: String,
    @SerialName("coverFileId") val coverFileId: String? = null,
    @SerialName("emojis") val emojis: List<EmojiInputDto>
)

@Serializable
data class UpdateEmojiPackRequestDto(
    @SerialName("name") val name: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("coverFileId") val coverFileId: String? = null,
    @SerialName("removeCover") val removeCover: Boolean? = null,
    @SerialName("emojis") val emojis: List<EmojiInputDto>? = null
)

@Serializable
data class EmojiPackIdDto(
    @SerialName("packId") val packId: String = ""
)

@Serializable
data class EmojiUploadInitRequestDto(
    @SerialName("name") val name: String,
    @SerialName("size") val size: Long,
    @SerialName("mimeType") val mimeType: String,
    @SerialName("packId") val packId: String,
    @SerialName("width") val width: Int? = null,
    @SerialName("height") val height: Int? = null
)

@Serializable
data class EmojiPackUsernameAvailabilityDto(
    @SerialName("available") val available: Boolean = false
)

@Serializable
data class EmojiFileDto(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("size") val size: Long = 0,
    @SerialName("mimeType") val mimeType: String = ""
)
