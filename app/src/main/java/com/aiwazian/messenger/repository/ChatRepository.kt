/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.network.api.ChatApi
import com.aiwazian.messenger.network.api.MessageApi
import com.aiwazian.messenger.network.dto.*
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val chatApi: ChatApi,
    private val messageApi: MessageApi
) {

    fun getAllChats(): Flow<List<Chat>> = flow {
        try {
            val response = chatApi.getAllChats()
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                emit(dtos.map { it.toDomain() })
            } else {
                Log.e("ChatRepository", "Failed to get all chats: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting all chats", e)
        }
    }

    suspend fun get(chatId: Long): Chat? {
        try {
            val response = chatApi.getChatById(chatId)
            if (response.isSuccessful) {
                return response.body()?.toDomain()
            } else {
                Log.e("ChatRepository", "Failed to get chat $chatId: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting chat $chatId", e)
        }
        return null
    }

    suspend fun getLastMessage(chatId: Long): Message? {
        try {
            val response = chatApi.getLastMessage(chatId)
            if (response.isSuccessful) {
                return response.body()?.toDomain()
            } else {
                Log.e("ChatRepository", "Failed to get last message for chat $chatId: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting last message for chat $chatId", e)
        }
        return null
    }

    suspend fun getMessages(chatId: Long): List<Message> {
        return try {
            val response = messageApi.getMessages(chatId)
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                dtos.map { it.toDomain() }
            } else {
                Log.e("ChatRepository", "Failed to get messages for chat $chatId: ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting messages for chat $chatId", e)
            emptyList()
        }
    }

    suspend fun sendMessage(chatId: Long, message: Message): Message? {
        return try {
            val request = TextMessageRequestDto(text = message.text ?: "")
            val response = messageApi.sendTextMessage(chatId, request)
            if (response.isSuccessful) {
                response.body()?.toDomain()
            } else {
                Log.e("ChatRepository", "Failed to send message: ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending message", e)
            null
        }
    }

    suspend fun initFileUpload(chatId: Long, dto: FileInitRequestDto): FileInitResponseDto? {
        return try {
            val response = messageApi.initFileUpload(chatId, dto)
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.e("ChatRepository", "Failed to init file upload: ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error initializing file upload", e)
            null
        }
    }

    suspend fun confirmFileUpload(chatId: Long, dto: FileConfirmRequestDto): Message? {
        return try {
            val response = messageApi.confirmFileUpload(chatId, dto)
            if (response.isSuccessful) {
                response.body()?.toDomain()
            } else {
                Log.e("ChatRepository", "Failed to confirm file upload: ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error confirming file upload", e)
            null
        }
    }

    suspend fun getDownloadUrl(chatId: Long, messageId: Int, fileId: String): FileDownloadResponseDto? {
        return try {
            val response = messageApi.getFileDownloadUrl(chatId, messageId, fileId)
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.e("ChatRepository", "Failed to get download url: ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting download url", e)
            null
        }
    }

    suspend fun makeAsRead(chatId: Long, messageId: Int): Boolean {
        return try {
            val response = messageApi.markRead(chatId, messageId)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error marking message as read", e)
            false
        }
    }

    suspend fun markAllAsRead(chatId: Long): Boolean {
        return try {
            val response = messageApi.markAllRead(chatId)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error marking all messages as read", e)
            false
        }
    }

    suspend fun deleteMessage(chatId: Long, messageId: Int, forEveryone: Boolean): Boolean {
        return try {
            val response = messageApi.deleteMessage(chatId, messageId, forEveryone)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error deleting message", e)
            false
        }
    }

    suspend fun deleteChatMessages(chatId: Long, forReceiver: Boolean): Boolean {
        return try {
            val response = messageApi.clearHistory(chatId)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error clearing chat history", e)
            false
        }
    }

    suspend fun archive(chatId: Long) {
        try {
            val response = chatApi.archiveChat(chatId)
            if (!response.isSuccessful) {
                Log.e("ChatRepository", "Failed to archive chat $chatId: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error archiving chat $chatId", e)
        }
    }

    suspend fun unarchive(chatId: Long) {
        try {
            val response = chatApi.unarchiveChat(chatId)
            if (!response.isSuccessful) {
                Log.e("ChatRepository", "Failed to unarchive chat $chatId: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error unarchiving chat $chatId", e)
        }
    }

    suspend fun deleteChat(chatId: Long): Boolean {
        return try {
            val response = chatApi.deleteChat(chatId)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error deleting chat $chatId", e)
            false
        }
    }

    suspend fun resetInviteLink(channelId: Long): String? {
        return try {
            val request = CreateInviteLinkRequestDto(channelId = channelId.toString())
            val response = chatApi.createInviteLink(request)
            if (response.isSuccessful) {
                response.body()?.link
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error resetting invite link", e)
            null
        }
    }
}
