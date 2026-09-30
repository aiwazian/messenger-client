/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.network.dto

import com.aiwazian.messenger.enums.CommentAuthorRole
import com.aiwazian.messenger.enums.MessageType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommentAuthorDto(
    @SerialName("id") val id: Long,
    @SerialName("firstName") val firstName: String = "",
    @SerialName("lastName") val lastName: String? = null
)

@Serializable
data class CommentDto(
    @SerialName("id") val id: Long,
    @SerialName("postId") val postId: Long,
    @SerialName("senderId") val senderId: Long,
    @SerialName("text") val text: String? = null,
    @SerialName("sendTime") val sendTime: Long,
    @SerialName("messageType") val messageType: MessageType = MessageType.TEXT,
    @SerialName("senderRole") val senderRole: CommentAuthorRole = CommentAuthorRole.MEMBER,
    @SerialName("sticker") val sticker: MessageStickerDto? = null,
    @SerialName("attachments") val attachments: List<MessageAttachmentDto> = emptyList(),
    @SerialName("sender") val sender: CommentAuthorDto? = null
)

@Serializable
data class CreateCommentRequestDto(
    @SerialName("text") val text: String? = null,
    @SerialName("stickerId") val stickerId: String? = null
)

@Serializable
data class ConfirmCommentRequestDto(
    @SerialName("attachments") val attachments: List<AttachmentInputDto>,
    @SerialName("text") val text: String? = null
)

@Serializable
data class NewCommentPayloadDto(
    @SerialName("chatId") val chatId: Long,
    @SerialName("postId") val postId: Long,
    @SerialName("comment") val comment: CommentDto
)

@Serializable
data class CommentsClearedPayloadDto(
    @SerialName("chatId") val chatId: Long
)
