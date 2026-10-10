/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WallpaperDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("fileId") val fileId: String = "",
    @SerialName("url") val url: String,
    @SerialName("name") val name: String? = null,
    @SerialName("mimeType") val mimeType: String? = null,
    @SerialName("darken") val darken: Int = 0,
    @SerialName("blur") val blur: Int = 0,
    @SerialName("offsetX") val offsetX: Float = 0f,
    @SerialName("offsetY") val offsetY: Float = 0f,
    @SerialName("source") val source: String? = null
)

@Serializable
data class WallpaperResponseDto(
    @SerialName("wallpaper") val wallpaper: WallpaperDto? = null
)

@Serializable
data class InitWallpaperUploadRequestDto(
    @SerialName("name") val name: String,
    @SerialName("size") val size: Long,
    @SerialName("mimeType") val mimeType: String,
    @SerialName("width") val width: Int? = null,
    @SerialName("height") val height: Int? = null
)

@Serializable
data class SetWallpaperRequestDto(
    @SerialName("fileId") val fileId: String,
    @SerialName("darken") val darken: Int,
    @SerialName("blur") val blur: Int,
    @SerialName("offsetX") val offsetX: Float,
    @SerialName("offsetY") val offsetY: Float
)

@Serializable
data class SetChatWallpaperRequestDto(
    @SerialName("scope") val scope: String,
    @SerialName("fileId") val fileId: String,
    @SerialName("darken") val darken: Int,
    @SerialName("blur") val blur: Int,
    @SerialName("offsetX") val offsetX: Float,
    @SerialName("offsetY") val offsetY: Float
)
