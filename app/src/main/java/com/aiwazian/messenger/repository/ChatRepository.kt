/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.database.dao.AttachmentDao
import com.aiwazian.messenger.database.dao.ChannelDao
import com.aiwazian.messenger.database.dao.ChatDao
import com.aiwazian.messenger.database.dao.GroupDao
import com.aiwazian.messenger.database.dao.MessageDao
import com.aiwazian.messenger.database.dao.UserDao
import com.aiwazian.messenger.database.entity.ChannelEntity
import com.aiwazian.messenger.database.entity.GroupEntity
import com.aiwazian.messenger.database.entity.UserEntity
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.enums.AttachmentType
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.network.api.ChatApi
import com.aiwazian.messenger.network.api.MessageApi
import com.aiwazian.messenger.network.dto.CreateInviteLinkRequestDto
import com.aiwazian.messenger.network.dto.FileConfirmRequestDto
import com.aiwazian.messenger.network.dto.FileDownloadResponseDto
import com.aiwazian.messenger.network.dto.FileInitRequestDto
import com.aiwazian.messenger.network.dto.FileInitResponseDto
import com.aiwazian.messenger.network.dto.TextMessageRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val chatApi: ChatApi,
    private val messageApi: MessageApi,
    private val messageDao: MessageDao,
    private val attachmentDao: AttachmentDao,
    private val chatDao: ChatDao,
    private val userDao: UserDao,
    private val groupDao: GroupDao,
    private val channelDao: ChannelDao
) {
    
    fun getAllChats(): Flow<List<Chat>> = channelFlow {
        launch {
            chatDao.getAllChatsFlow().collectLatest { entities ->
                val result = entities.map { entity ->
                    val name = when (ChatType.fromId(entity.chatId)) {
                        ChatType.PRIVATE -> userDao.get(entity.chatId)
                            ?.let { "${it.firstName} ${it.lastName.orEmpty()}".trim() } ?: ""
                        
                        ChatType.GROUP -> groupDao.getById(entity.chatId)?.name ?: ""
                        ChatType.CHANNEL -> channelDao.get(entity.chatId)?.name ?: ""
                        else -> ""
                    }
                    
                    val lastMessage = entity.lastMessageId?.let {
                        messageDao.getMessageById(it)?.toDomain()
                    }
                    
                    entity.toDomain(name, lastMessage)
                }
                send(result)
            }
        }
        
        try {
            val response = chatApi.getAllChats()
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                val chatIds = dtos.map { it.id.toLong() }
                
                chatDao.deleteChatsNotIn(chatIds)
                messageDao.deleteMessagesNotInChatIds(chatIds)
                
                chatDao.upsertChats(dtos.map { it.toEntity() })
                
                val lastMessages = dtos.mapNotNull { it.lastMessage?.toDomain() }
                saveMessagesToDb(lastMessages)
                
                dtos.forEach { dto ->
                    val id = dto.id.toLong()
                    val type = ChatType.fromId(id)
                    when (type) {
                        ChatType.PRIVATE -> {
                            val user = userDao.get(id) ?: UserEntity(id)
                            userDao.insert(user.copy(firstName = dto.name))
                        }
                        
                        ChatType.GROUP -> {
                            val group =
                                groupDao.getById(id) ?: GroupEntity(
                                    id,
                                    null,
                                    dto.name,
                                    null,
                                    null,
                                    0,
                                    0
                                )
                            groupDao.insert(group.copy(name = dto.name))
                        }
                        
                        ChatType.CHANNEL -> {
                            val channel =
                                channelDao.get(id) ?: ChannelEntity(
                                    id,
                                    dto.name,
                                    null,
                                    null,
                                    0,
                                    null,
                                    0,
                                    null,
                                    null
                                )
                            channelDao.insert(channel.copy(name = dto.name))
                        }
                        
                        else -> {}
                    }
                }
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
                Log.e(
                    "ChatRepository",
                    "Failed to get last message for chat $chatId: ${response.message()}"
                )
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting last message for chat $chatId", e)
        }
        return null
    }
    
    fun getMessagesFlow(
        senderId: Long,
        chatId: Long,
        limit: Int,
        offset: Int
    ): Flow<List<Message>> {
        return when (ChatType.fromId(chatId)) {
            ChatType.PRIVATE -> messageDao.getMessages(senderId, chatId, limit, offset)
                .map { entities ->
                    entities.map { it.message.toDomain(it.messageAttachments.map { att -> att.toDomain() }) }
                }
            
            else -> messageDao.getMessages(chatId, limit, offset).map { entities ->
                entities.map { it.message.toDomain(it.messageAttachments.map { att -> att.toDomain() }) }
            }
        }
    }
    
    suspend fun getMessages(chatId: Long, limit: Int? = null, offset: Int? = null): List<Message> {
        return try {
            val response = messageApi.getMessages(chatId, limit, offset)
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                val domains = dtos.map { it.toDomain() }
                saveMessagesToDb(domains)
                domains
            } else {
                Log.e(
                    "ChatRepository",
                    "Failed to get messages for chat $chatId: ${response.message()}"
                )
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
                val sentMessage = response.body()?.toDomain()
                sentMessage?.let { saveMessagesToDb(listOf(it)) }
                sentMessage
            } else {
                Log.e("ChatRepository", "Failed to send message: ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending message", e)
            null
        }
    }
    
    suspend fun saveMessage(message: Message) {
        saveMessagesToDb(listOf(message))
    }
    
    private suspend fun saveMessagesToDb(messages: List<Message>) {
        messageDao.saveMessages(messages.map { it.toEntity() })
        messages.forEach { msg ->
            val attachments =
                msg.files.map { it.toEntity(msg.id.toLong(), AttachmentType.MESSAGE, msg.chatId) }
            attachmentDao.upsertAttachments(attachments)
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
    
    suspend fun getDownloadUrl(
        chatId: Long,
        messageId: Int,
        fileId: String
    ): FileDownloadResponseDto? {
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
            if (response.isSuccessful) {
                chatDao.deleteChat(chatId)
                messageDao.clearChatHistory(chatId)
            }
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error deleting chat $chatId", e)
            false
        }
    }
    
    suspend fun saveChat(chat: Chat) {
        chatDao.upsertChats(listOf(chat.toEntity()))
    }
    
    suspend fun syncChat(chat: Chat) {
        chatDao.upsertChats(listOf(chat.toEntity()))
        
        val id = chat.id
        val type = ChatType.fromId(id)
        when (type) {
            ChatType.PRIVATE -> {
                val user = userDao.get(id) ?: UserEntity(id)
                userDao.insert(user.copy(firstName = chat.chatName))
            }
            
            ChatType.GROUP -> {
                val group =
                    groupDao.getById(id) ?: GroupEntity(id, null, chat.chatName, null, null, 0, 0)
                groupDao.insert(group.copy(name = chat.chatName))
            }
            
            ChatType.CHANNEL -> {
                val channel =
                    channelDao.get(id) ?: ChannelEntity(
                        id,
                        chat.chatName,
                        null,
                        null,
                        0,
                        null,
                        0,
                        null,
                        null
                    )
                channelDao.insert(channel.copy(name = chat.chatName))
            }
            
            else -> {}
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
