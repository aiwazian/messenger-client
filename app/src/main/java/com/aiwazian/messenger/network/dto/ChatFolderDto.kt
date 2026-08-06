/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import com.aiwazian.messenger.enums.ChatFolderCategory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatFolderDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("sortOrder") val sortOrder: Int = 0,
    @SerialName("categories") val categories: List<ChatFolderCategory> = emptyList(),
    @SerialName("chats") val chats: List<ChatFolderChatDto> = emptyList()
)

@Serializable
data class ChatFolderChatDto(
    @SerialName("chatId") val chatId: String,
    @SerialName("isIncluded") val isIncluded: Boolean = true,
    @SerialName("isPinned") val isPinned: Boolean = false,
    @SerialName("sortOrder") val sortOrder: Int = 0
)

@Serializable
data class CreateChatFolderRequestDto(
    @SerialName("name") val name: String,
    @SerialName("chatIds") val chatIds: List<String> = emptyList(),
    @SerialName("categories") val categories: List<ChatFolderCategory> = emptyList()
)

@Serializable
data class UpdateChatFolderRequestDto(
    @SerialName("name") val name: String? = null,
    @SerialName("chatIds") val chatIds: List<String>? = null,
    @SerialName("categories") val categories: List<ChatFolderCategory>? = null
)

@Serializable
data class PinFolderChatsRequestDto(
    @SerialName("chatIds") val chatIds: List<String>
)

@Serializable
data class ReorderChatFoldersRequestDto(
    @SerialName("folderIds") val folderIds: List<Int>
)
