/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.R
import com.aiwazian.messenger.database.dao.AttachmentDao
import com.aiwazian.messenger.database.dao.ChatDao
import com.aiwazian.messenger.database.dao.MessageDao
import com.aiwazian.messenger.database.entity.ChatEntity
import com.aiwazian.messenger.database.entity.FileEntity
import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.enums.ChatType
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.network.api.ChatApi
import com.aiwazian.messenger.network.api.MessageApi
import com.aiwazian.messenger.network.dto.AttachmentInputDto
import com.aiwazian.messenger.network.dto.ClearHistoryRequestDto
import com.aiwazian.messenger.network.dto.DeleteMessageRequestDto
import com.aiwazian.messenger.network.dto.FileConfirmRequestDto
import com.aiwazian.messenger.network.dto.FileInitRequestDto
import com.aiwazian.messenger.network.dto.FileInitResponseDto
import com.aiwazian.messenger.network.dto.PinChatsRequestDto
import com.aiwazian.messenger.network.dto.TextMessageRequestDto
import com.aiwazian.messenger.socket.WebSocketClient
import com.aiwazian.messenger.utils.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val chatApi: ChatApi,
    private val messageApi: MessageApi,
    private val messageDao: MessageDao,
    private val attachmentDao: AttachmentDao,
    private val fileRepository: FileRepository,
    private val chatDao: ChatDao,
    private val userRepository: UserRepository,
    private val channelRepository: ChannelRepository,
    private val groupRepository: GroupRepository,
    private val socket: WebSocketClient
) {
    
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllChats(): Flow<List<Chat>> = chatDao.getAllChatsFlow().flatMapLatest { chatEntities ->
        val myId =
            userRepository.getMe().firstOrNull()?.id ?: return@flatMapLatest flowOf(emptyList())
        
        if (chatEntities.isEmpty()) return@flatMapLatest flowOf(emptyList())
        
        val flows = chatEntities.map { chatEntity ->
            messageDao.getLastMessageForChatFlow(myId, chatEntity.chatId)
                .map { messageWithAttachments ->
                    val name = resolveChatName(chatEntity, myId) ?: return@map null
                    
                    val lastMessage = messageWithAttachments?.let {
                        val attachments = it.attachments.map { att -> att.toDomain() }
                        it.message.toDomain(attachments)
                    }
                    
                    chatEntity.toDomain(name, lastMessage)
                }
        }
        
        combine(flows) { chatsArray ->
            chatsArray.filterNotNull().sortedWith(
                compareByDescending<Chat> { it.isPinned }
                    .thenByDescending { it.lastMessage?.sendTime ?: 0L }
            )
        }
    }.onStart {
        try {
            refreshChats()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Ошибка обновления чатов в onStart", e)
        }
    }
    
    private suspend fun resolveChatName(chatEntity: ChatEntity, myId: Long): UiText? {
        return when (ChatType.fromId(chatEntity.chatId)) {
            ChatType.PRIVATE -> {
                if (chatEntity.chatId == myId) {
                    UiText.StringResource(R.string.saved_messages)
                } else {
                    userRepository.getById(chatEntity.chatId).firstOrNull()?.let { user ->
                        UiText.DynamicString("${user.firstName} ${user.lastName.orEmpty()}".trim())
                    }
                }
            }
            
            ChatType.GROUP -> {
                groupRepository.getById(chatEntity.chatId).firstOrNull()?.let {
                    UiText.DynamicString(it.name)
                }
            }
            
            ChatType.CHANNEL -> {
                channelRepository.getById(chatEntity.chatId).firstOrNull()?.let {
                    UiText.DynamicString(it.name)
                }
            }
            
            else -> UiText.DynamicString("")
        }
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
            
            ChatType.GROUP -> messageDao.getMessagesWithAttachments(chatId, limit, offset)
                .map { list ->
                    list.map { messageWithAttachments ->
                        val attachments = messageWithAttachments.attachments.map { attWithFile ->
                            attWithFile.toDomain()
                        }
                        messageWithAttachments.message.toDomain(attachments)
                    }
                }
            
            ChatType.PRIVATE -> messageDao.getMessagesWithAttachments(
                senderId, chatId, limit, offset
            )
                .map { list ->
                    list.map { messageWithAttachments ->
                        val attachments = messageWithAttachments.attachments.map { attWithFile ->
                            attWithFile.toDomain()
                        }
                        messageWithAttachments.message.toDomain(attachments)
                    }
                }
            
            else -> emptyFlow()
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
        val tempId = -System.currentTimeMillis()
        val senderId = if (ChatType.fromId(chatId) == ChatType.CHANNEL) chatId
        else userRepository.getMe().first().id
        
        val localMessage = Message(
            id = tempId,
            senderId = senderId,
            chatId = chatId,
            text = message,
            sendTime = System.currentTimeMillis(),
            isRead = false,
            messageType = MessageType.TEXT,
            systemMessageEventType = null,
            attachments = emptyList()
        )
        
        saveLocalMessage(localMessage)
        
        return try {
            val request = TextMessageRequestDto(text = message)
            val response = messageApi.sendTextMessage(chatId, request, socket.socketId.orEmpty())
            if (response.isSuccessful) {
                val sentMessage = response.body()?.toDomain()
                sentMessage?.let {
                    updateMessageId(tempId, it.id)
                }
                deleteLocalMessage(tempId)
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
    
    suspend fun saveLocalMessage(message: Message) {
        saveMessagesToDb(listOf(message))
    }
    
    private suspend fun saveMessagesToDb(messages: List<Message>) {
        messageDao.saveMessages(messages.map { it.toEntity() })
        messages.forEach { message ->
            val attachments = message.attachments.map { attachment ->
                val existingFile = fileRepository.getById(attachment.fileId)
                
                val file = if (existingFile != null) {
                    existingFile
                } else {
                    val newFile = FileEntity(
                        id = attachment.fileId,
                        name = attachment.name,
                        size = attachment.size,
                        path = null,
                        status = attachment.status
                    )
                    fileRepository.save(newFile)
                    newFile
                }
                
                attachment.toEntity(file)
            }
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
    
    suspend fun confirmFileUpload(
        chatId: Long,
        attachments: List<AttachmentInputDto>,
        text: String? = null
    ): Result<Message> {
        return try {
            val dto = FileConfirmRequestDto(attachments = attachments, text = text)
            val response = messageApi.confirmFileUpload(chatId, dto, socket.socketId.orEmpty())
            if (response.isSuccessful) {
                val sentMessage = response.body()?.toDomain()
                if (sentMessage != null) {
                    Result.success(sentMessage)
                } else {
                    Result.failure(Exception(""))
                }
            } else {
                Log.e("ChatRepository", "Failed to confirm file upload: ${response.message()}")
                Result.failure(Exception(""))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error confirming file upload", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateMessageId(oldId: Long, newId: Long) {
        messageDao.updateMessageId(oldId, newId)
    }
    
    suspend fun getDownloadUrl(chatId: Long, messageId: Long, fileId: String): String? {
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
    
    suspend fun deleteMessage(
        chatId: Long,
        messageId: Long,
        deleteForRecipient: Boolean = false
    ): Result<Unit> {
        return try {
            val response = messageApi.deleteMessage(
                chatId,
                messageId,
                DeleteMessageRequestDto(deleteForRecipient)
            )
            if (response.isSuccessful) {
                deleteLocalMessage(messageId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error deleting message", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteLocalMessage(messageId: Long) {
        messageDao.deleteMessageById(messageId)
    }
    
    suspend fun clearLocalHistory(chatId: Long) {
        userRepository.getMe().firstOrNull()?.id?.let { userId ->
            messageDao.clearChatHistory(userId, chatId)
        }
    }
    
    suspend fun deleteChatMessages(chatId: Long, clearForRecipient: Boolean = false): Boolean {
        return try {
            val response = messageApi.clearHistory(chatId, ClearHistoryRequestDto(clearForRecipient))
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
    
    suspend fun deleteLocalChat(chatId: Long) {
        chatDao.deleteChat(chatId)
        clearLocalHistory(chatId)
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
    
    suspend fun pinChats(chatIds: List<Long>): Boolean {
        return try {
            val request = PinChatsRequestDto(chatIds.map { it.toString() })
            val response = chatApi.pinChats(request)
            if (response.isSuccessful) {
                chatDao.updatePinnedStatus(chatIds, true)
                true
            } else {
                Log.e("ChatRepository", "Failed to pin chats: ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error pinning chats", e)
            false
        }
    }
    
    suspend fun unpinChats(chatIds: List<Long>): Boolean {
        return try {
            val request = PinChatsRequestDto(chatIds.map { it.toString() })
            val response = chatApi.unpinChats(request)
            if (response.isSuccessful) {
                chatDao.updatePinnedStatus(chatIds, false)
                true
            } else {
                Log.e("ChatRepository", "Failed to unpin chats: ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error unpinning chats", e)
            false
        }
    }
}
