/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.database.dao.MessageDao
import com.aiwazian.messenger.database.dao.MessagePinDao
import com.aiwazian.messenger.database.entity.MessagePinEntity
import com.aiwazian.messenger.domain.MessagePinPayload
import com.aiwazian.messenger.domain.PinnedMessage
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.network.api.MessageApi
import com.aiwazian.messenger.network.dto.PinMessageRequestDto
import com.aiwazian.messenger.socket.WebSocketClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatPinnedMessagesRepository @Inject constructor(
    private val messageApi: MessageApi,
    private val messageDao: MessageDao,
    private val messagePinDao: MessagePinDao,
    private val userRepository: UserRepository,
    private val socket: WebSocketClient
) {

    fun observePinnedMessages(chatId: Long): Flow<List<PinnedMessage>> {
        return messagePinDao.observeChatPins(chatId).flatMapLatest { pins ->
            if (pins.isEmpty()) {
                flowOf(emptyList())
            } else {
                messageDao.getMessagesByIds(pins.map { it.messageId }).map { messages ->
                    val byId = messages.associateBy { it.message.id }

                    pins.mapNotNull { pin ->
                        byId[pin.messageId]?.let { messageWithAttachments ->
                            val attachments = messageWithAttachments.attachments
                                .sortedBy { it.attachment.sortOrder }
                                .map { it.toDomain() }
                            PinnedMessage(
                                messageId = pin.messageId,
                                chatId = pin.chatId,
                                forEveryone = pin.forEveryone,
                                pinnedAt = pin.pinnedAt,
                                message = messageWithAttachments.message.toDomain(attachments)
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun pinMessage(
        chatId: Long,
        messageId: Long,
        forEveryone: Boolean
    ): Result<Unit> {
        return try {
            val response = messageApi.pinMessage(
                chatId = chatId,
                messageId = messageId,
                request = PinMessageRequestDto(forEveryone = forEveryone),
                socketId = socket.socketId.orEmpty()
            )

            val pin = response.body()
            if (response.isSuccessful && pin != null) {
                upsertLocalPin(
                    chatId = pin.chatId,
                    messageId = pin.messageId,
                    forEveryone = pin.forEveryone,
                    pinnedAt = pin.pinnedAt
                )
                Result.success(Unit)
            } else {
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pinning message $messageId in chat $chatId", e)
            Result.failure(e)
        }
    }

    /**
     * Открепление сообщения.
     *
     * Сервер снимает личное закрепление всегда, а общее — когда открепляющий
     * вправе: в личном чате или с правом закрепления. includeShared удаляет
     * локальную строку общего закрепления сразу: свой сокет исключён из
     * рассылки и события о снятии не получит.
     */
    suspend fun unpinMessage(
        chatId: Long,
        messageId: Long,
        includeShared: Boolean = false
    ): Result<Unit> {
        return try {
            val response = messageApi.unpinMessage(
                chatId = chatId,
                messageId = messageId,
                socketId = socket.socketId.orEmpty()
            )

            if (response.isSuccessful) {
                messagePinDao.deletePin(
                    chatId = chatId,
                    messageId = messageId,
                    forEveryone = false
                )

                if (includeShared) {
                    messagePinDao.deletePin(
                        chatId = chatId,
                        messageId = messageId,
                        forEveryone = true
                    )
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unpinning message $messageId in chat $chatId", e)
            Result.failure(e)
        }
    }

    /** Свежие закрепления с сервера вместо локального кэша: вызывается при открытии чата. */
    suspend fun refreshPinnedMessages(chatId: Long) {
        try {
            val response = messageApi.getPinnedMessages(chatId)
            val pins = response.body()
            if (!response.isSuccessful || pins == null) return

            val myId = userRepository.getMe().firstOrNull()?.id ?: return

            messageDao.saveMessages(pins.mapNotNull { pin -> pin.message?.toDomain() }.map { it.toEntity() })

            messagePinDao.deleteChatPins(chatId)
            messagePinDao.upsertPins(pins.map { pin ->
                MessagePinEntity(
                    chatId = pin.chatId,
                    messageId = pin.messageId,
                    forEveryone = pin.forEveryone,
                    pinnedAt = pin.pinnedAt,
                    ownerId = myId
                )
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing pinned messages for chat $chatId", e)
        }
    }

    suspend fun applyRemotePin(payload: MessagePinPayload) {
        upsertLocalPin(
            chatId = payload.chatId,
            messageId = payload.messageId,
            forEveryone = payload.forEveryone,
            pinnedAt = payload.pinnedAt
        )
    }

    suspend fun applyRemoteUnpin(payload: MessagePinPayload) {
        messagePinDao.deletePin(
            chatId = payload.chatId,
            messageId = payload.messageId,
            forEveryone = payload.forEveryone
        )
    }

    private suspend fun upsertLocalPin(
        chatId: Long,
        messageId: Long,
        forEveryone: Boolean,
        pinnedAt: Long
    ) {
        val myId = userRepository.getMe().firstOrNull()?.id ?: return

        messagePinDao.upsertPins(
            listOf(
                MessagePinEntity(
                    chatId = chatId,
                    messageId = messageId,
                    forEveryone = forEveryone,
                    pinnedAt = pinnedAt,
                    ownerId = myId
                )
            )
        )
    }

    private companion object {
        const val TAG = "ChatPinnedMessagesRepo"
    }
}
