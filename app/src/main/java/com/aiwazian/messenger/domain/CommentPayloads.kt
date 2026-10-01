/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.network.dto.CommentsClearedPayloadDto
import com.aiwazian.messenger.network.dto.DeleteCommentPayloadDto
import com.aiwazian.messenger.network.dto.NewCommentPayloadDto

data class NewCommentPayload(
    val chatId: Long,
    val postId: Long,
    val comment: Comment
) {
    companion object {
        fun fromDto(dto: NewCommentPayloadDto) = NewCommentPayload(
            chatId = dto.chatId,
            postId = dto.postId,
            comment = dto.comment.toDomain()
        )
    }
}

data class CommentsClearedPayload(
    val chatId: Long
) {
    companion object {
        fun fromDto(dto: CommentsClearedPayloadDto) = CommentsClearedPayload(
            chatId = dto.chatId
        )
    }
}

data class CommentDeletedPayload(
    val chatId: Long,
    val postId: Long,
    val commentId: Long
) {
    companion object {
        fun fromDto(dto: DeleteCommentPayloadDto) = CommentDeletedPayload(
            chatId = dto.chatId,
            postId = dto.postId,
            commentId = dto.commentId
        )
    }
}
