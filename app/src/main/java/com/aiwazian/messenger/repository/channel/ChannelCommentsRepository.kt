/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository.channel

import android.util.Log
import android.net.Uri
import com.aiwazian.messenger.database.dao.ChannelDao
import com.aiwazian.messenger.domain.Channel
import com.aiwazian.messenger.domain.Comment
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.network.api.ChannelApi
import com.aiwazian.messenger.network.dto.AttachmentInputDto
import com.aiwazian.messenger.network.dto.ConfirmCommentRequestDto
import com.aiwazian.messenger.network.dto.CreateCommentRequestDto
import com.aiwazian.messenger.network.dto.EditCommentRequestDto
import com.aiwazian.messenger.network.dto.FileDownloadResponseDto
import com.aiwazian.messenger.network.dto.FileInitRequestDto
import com.aiwazian.messenger.network.dto.FileInitResponseDto
import com.aiwazian.messenger.network.dto.SetCommentsEnabledRequestDto
import com.aiwazian.messenger.network.dto.SetCommentsRestrictedRequestDto
import com.aiwazian.messenger.socket.WebSocketClient
import javax.inject.Inject

class ChannelCommentsRepository @Inject constructor(
    private val channelApi: ChannelApi,
    private val channelDao: ChannelDao,
    private val webSocketClient: WebSocketClient
) {

    suspend fun getComments(channelId: Long, postId: Long): Result<List<Comment>> {
        return try {
            val response = channelApi.getPostComments(channelId, postId)
            if (response.isSuccessful) {
                Result.success(response.body().orEmpty().map { it.toDomain() })
            } else {
                Result.failure(Exception("Get comments failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting comments", e)
            Result.failure(e)
        }
    }

    suspend fun sendComment(
        channelId: Long,
        postId: Long,
        text: String?,
        stickerId: Long? = null,
        replyToId: Long? = null
    ): Result<Comment> {
        return try {
            val response = channelApi.createPostComment(
                channelId,
                postId,
                CreateCommentRequestDto(
                    text = text?.takeIf { it.isNotBlank() },
                    stickerId = stickerId?.toString(),
                    replyToId = replyToId?.toString()
                ),
                webSocketClient.socketId.orEmpty()
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.toDomain())
            } else {
                Result.failure(Exception("Send comment failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending comment", e)
            Result.failure(e)
        }
    }

    suspend fun editComment(
        channelId: Long,
        postId: Long,
        commentId: Long,
        text: String
    ): Result<Comment> {
        return try {
            val response = channelApi.editComment(
                channelId,
                postId,
                commentId,
                EditCommentRequestDto(text),
                webSocketClient.socketId.orEmpty()
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.toDomain())
            } else {
                Result.failure(Exception("Edit comment failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error editing comment", e)
            Result.failure(e)
        }
    }

    suspend fun deleteComment(
        channelId: Long,
        postId: Long,
        commentId: Long
    ): Result<Unit> {
        return try {
            val response = channelApi.deleteComment(
                channelId,
                postId,
                commentId,
                webSocketClient.socketId.orEmpty()
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete comment failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting comment", e)
            Result.failure(e)
        }
    }

    suspend fun initFileUpload(
        channelId: Long,
        postId: Long,
        request: FileInitRequestDto
    ): FileInitResponseDto? {
        return try {
            val response = channelApi.initCommentFileUpload(channelId, postId, request)
            val body = response.body()
            if (response.isSuccessful && body != null) body else null
        } catch (e: Exception) {
            Log.e(TAG, "Error init comment upload", e)
            null
        }
    }

    suspend fun confirmFiles(
        channelId: Long,
        postId: Long,
        attachments: List<AttachmentInputDto>,
        text: String?
    ): Result<Comment> {
        return try {
            val response = channelApi.confirmCommentFiles(
                channelId,
                postId,
                ConfirmCommentRequestDto(attachments = attachments, text = text?.takeIf { it.isNotBlank() }),
                webSocketClient.socketId.orEmpty()
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.toDomain())
            } else {
                Result.failure(Exception("Confirm comment files failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error confirming comment files", e)
            Result.failure(e)
        }
    }

    suspend fun getFileDownloadUrl(
        channelId: Long,
        postId: Long,
        commentId: Long,
        fileId: String
    ): Result<String> {
        return try {
            val response = channelApi.getCommentFileDownloadUrl(channelId, postId, commentId, fileId)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.downloadUrl)
            } else {
                Result.failure(Exception("Get comment file url failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting comment file url", e)
            Result.failure(e)
        }
    }

    /**
     * Меняет комментирование постов канала.
     *
     * @param channel текущее состояние канала, нужно для обновления кэша без потери
     * остальных полей.
     */
    suspend fun setCommentsEnabled(channel: Channel, enabled: Boolean): Result<Unit> {
        return try {
            val response =
                channelApi.setCommentsEnabled(channel.id, SetCommentsEnabledRequestDto(enabled))
            if (response.isSuccessful) {
                channelDao.insert(channel.copy(commentsEnabled = enabled).toEntity())
                Result.success(Unit)
            } else {
                Result.failure(Exception("Update comments enabled failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating comments enabled", e)
            Result.failure(e)
        }
    }

    suspend fun setCommentsRestrictedToSubscribers(
        channel: Channel,
        restricted: Boolean
    ): Result<Unit> {
        return try {
            val response = channelApi.setCommentsRestrictedToSubscribers(
                channel.id,
                SetCommentsRestrictedRequestDto(restricted)
            )
            if (response.isSuccessful) {
                channelDao.insert(
                    channel.copy(commentsRestrictedToSubscribers = restricted).toEntity()
                )
                Result.success(Unit)
            } else {
                Result.failure(Exception("Update comments restriction failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating comments restriction", e)
            Result.failure(e)
        }
    }

    suspend fun deleteAllComments(channelId: Long): Result<Unit> {
        return try {
            val response = channelApi.deleteAllComments(channelId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete all comments failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all comments", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "ChannelCommentsRepository"
    }
}
