/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.R
import com.aiwazian.messenger.database.dao.AttachmentDao
import com.aiwazian.messenger.database.dao.ChatDao
import com.aiwazian.messenger.database.dao.FileDao
import com.aiwazian.messenger.database.dao.MessageDao
import com.aiwazian.messenger.database.entity.FileEntity
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.DownloadStatus
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.network.api.ChatApi
import com.aiwazian.messenger.network.api.MessageApi
import com.aiwazian.messenger.network.dto.AttachmentInputDto
import com.aiwazian.messenger.network.dto.FileConfirmRequestDto
import com.aiwazian.messenger.network.dto.FileInitRequestDto
import com.aiwazian.messenger.network.dto.FileInitResponseDto
import com.aiwazian.messenger.network.dto.TextMessageRequestDto
import com.aiwazian.messenger.utils.UiText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val chatApi: ChatApi,
    private val messageApi: MessageApi,
    private val messageDao: MessageDao,
    private val attachmentDao: AttachmentDao,
    private val fileDao: FileDao,
    private val chatDao: ChatDao,
    private val userRepository: UserRepository,
    private val channelRepository: ChannelRepository,
    private val groupRepository: GroupRepository
) {
    
    fun getAllChats(): Flow<List<Chat>> = channelFlow {
        launch {
            chatDao.getAllChatsFlow().collectLatest { chatEntities ->
                val myId = userRepository.getMe().first().id
                val result = chatEntities.map { chatEntity ->
                    val name: UiText = when (ChatType.fromId(chatEntity.chatId)) {
                        ChatType.PRIVATE -> {
                            if (chatEntity.chatId == myId) {
                                UiText.StringResource(R.string.saved_messages)
                            } else {
                                userRepository.getById(chatEntity.chatId)
                                    .first()
                                    .let { UiText.DynamicString("${it.firstName} ${it.lastName.orEmpty()}".trim()) }
                            }
                        }
                        
                        ChatType.GROUP -> UiText.DynamicString(
                            groupRepository.getById(chatEntity.chatId).first().name
                        )
                        
                        ChatType.CHANNEL -> UiText.DynamicString(
                            channelRepository.getById(chatEntity.chatId).first().name
                        )
                        
                        else -> UiText.DynamicString("")
                    }
                    
                    val lastMessage = chatEntity.lastMessageId?.let {
                        messageDao.getMessageById(it)?.let { messageWithAttachments ->
                            val attachments =
                                messageWithAttachments.attachments.map { attWithFile ->
                                    attWithFile.toDomain()
                                }
                            messageWithAttachments.message.toDomain(attachments)
                        }
                    }
                    chatEntity.toDomain(name, lastMessage)
                }
                send(result)
            }
        }
        refreshChats()
    }
    
    suspend fun refreshChats() {
        try {
            val response = chatApi.getAllChats()
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                val chatIds = dtos.map { it.id }
                
                chatDao.deleteChatsNotIn(chatIds)
                messageDao.deleteMessagesNotInChatIds(chatIds)
                
                chatDao.upsertChats(dtos.map { it.toEntity() })
                
                val lastMessages = dtos.mapNotNull { it.lastMessage?.toDomain() }
                saveMessagesToDb(lastMessages)
            } else {
                Log.e("ChatRepository", "Failed to get all chats: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting all chats", e)
        }
    }
    
    fun getMessagesFlow(
        senderId: Long,
        chatId: Long,
        limit: Int,
        offset: Int
    ): Flow<List<Message>> {
        return when (ChatType.fromId(chatId)) {
            ChatType.CHANNEL -> messageDao.getMessagesWithAttachments(chatId, chatId, limit, offset)
                .map { list ->
                    list.map { messageWithAttachments ->
                        val attachments = messageWithAttachments.attachments.map { attWithFile ->
                            attWithFile.toDomain()
                        }
                        messageWithAttachments.message.toDomain(attachments)
                    }
                }
            
            else -> messageDao.getMessagesWithAttachments(senderId, chatId, limit, offset)
                .map { list ->
                    list.map { messageWithAttachments ->
                        val attachments = messageWithAttachments.attachments.map { attWithFile ->
                            attWithFile.toDomain()
                        }
                        messageWithAttachments.message.toDomain(attachments)
                    }
                }
        }
    }
    
    suspend fun getMessages(chatId: Long, limit: Int? = null, offset: Int? = null): List<Message> {
        return try {
            val response = messageApi.getMessages(chatId, limit, offset)
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                val messages = dtos.map { messageDto ->
                    messageDto.toDomain()
                }
                saveMessagesToDb(messages)
                messages
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
    
    fun getById(chatId: Long): Flow<Chat?> {
        return chatDao.getChatByIdFlow(chatId).map {
            it?.toDomain(UiText.DynamicString(""), null)
        }
    }
    
    suspend fun sendMessage(chatId: Long, message: String): Message? {
        return try {
            val request = TextMessageRequestDto(text = message)
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
    
    suspend fun markMessageAsRead(chatId: Long, messageId: Int) {
        messageDao.updateMessageReadStatus(
            messageId,
            true
        ) // Не вызываем API тут, т.к. это задача синхронизатора или ViewModel (в зависимости от логики)
        // Однако для текущей реализации лучше оставить API вызов в makeAsRead,
        // а этот метод использовать для локального обновления.
    }
    
    suspend fun saveMessage(message: Message) {
        saveMessagesToDb(listOf(message))
    }
    
    private suspend fun saveMessagesToDb(messages: List<Message>) {
        messageDao.saveMessages(messages.map { it.toEntity() })
        messages.forEach { message ->
            val attachments = message.attachments.map { attachment ->
                val existingFile = fileDao.getById(attachment.fileId)
                
                val file = if (existingFile == null) {
                    val newFile = FileEntity(
                        id = attachment.fileId,
                        name = attachment.name,
                        size = attachment.size,
                        path = null,
                        status = DownloadStatus.COMPLETED
                    )
                    fileDao.save(newFile)
                    newFile
                } else {
                    existingFile
                }
                
                attachment.toEntity(file)
            }
            attachmentDao.upsertAttachments(attachments)
            if (ChatType.fromId(message.chatId) == ChatType.PRIVATE) {
                chatDao.updateLastMessageId(message.senderId, message.id)
            } else {
                chatDao.updateLastMessageId(message.chatId, message.id)
            }
        }
    }
    
    suspend fun updateFileStatus(fileId: String, status: DownloadStatus, path: String? = null) {
        fileDao.updateStatusAndPath(fileId, status, path)
    }
    
    suspend fun deleteFile(fileId: String) {
        fileDao.deleteFile(fileId)
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
    
    suspend fun confirmFileUpload(chatId: Long, attachments: List<AttachmentInputDto>, text: String? = null): Message? {
        return try {
            val dto = FileConfirmRequestDto(attachments = attachments, text = text)
            val response = messageApi.confirmFileUpload(chatId, dto)
            if (response.isSuccessful) {
                val sentMessage = response.body()?.toDomain()
                sentMessage?.let { saveMessagesToDb(listOf(it)) }
                sentMessage
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
        chatId: Long, messageId: Long, fileId: String
    ): String? {
        return try {
            val response = messageApi.getFileDownloadUrl(chatId, messageId, fileId)
            if (response.isSuccessful) {
                response.body()?.downloadUrl
            } else {
                Log.e("ChatRepository", "Failed to get download url: ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting download url", e)
            null
        }
    }
    
    suspend fun makeAsRead(chatId: Long, messageId: Long): Boolean {
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
    
    suspend fun deleteMessage(chatId: Long, messageId: Long): Boolean {
        return try {
            messageDao.deleteMessageById(messageId)
            val response = messageApi.deleteMessage(chatId, messageId)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error deleting message", e)
            false
        }
    }
    
    suspend fun deleteLocalMessage(messageId: Long) {
        messageDao.deleteMessageById(messageId)
    }
    
    suspend fun clearLocalHistory(chatId: Long) {
        userRepository.getMe().first().id.let { userId ->
            messageDao.clearChatHistory(userId, chatId)
        }
    }
    
    suspend fun deleteChatMessages(chatId: Long): Boolean {
        return try {
            val response = messageApi.clearHistory(chatId)
            clearLocalHistory(chatId)
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
                Log.e(
                    "ChatRepository", "Failed to unarchive chat $chatId: ${response.message()}"
                )
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error unarchiving chat $chatId", e)
        }
    }
    
    suspend fun deleteChat(chatId: Long): Boolean {
        return try {
            val response = chatApi.deleteChat(chatId)
            chatDao.deleteChat(chatId)
            clearLocalHistory(chatId)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error deleting chat $chatId", e)
            false
        }
    }
}
