/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.net.Uri
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
import com.aiwazian.messenger.enums.MessageStatus
import com.aiwazian.messenger.enums.MessageType
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.network.api.ChatApi
import com.aiwazian.messenger.network.api.MessageApi
import com.aiwazian.messenger.network.dto.AttachmentInputDto
import com.aiwazian.messenger.network.dto.ClearHistoryRequestDto
import com.aiwazian.messenger.network.dto.DeleteChatRequestDto
import com.aiwazian.messenger.network.dto.DeleteMessageRequestDto
import com.aiwazian.messenger.network.dto.EditMessageRequestDto
import com.aiwazian.messenger.network.dto.FileConfirmRequestDto
import com.aiwazian.messenger.network.dto.FileInitRequestDto
import com.aiwazian.messenger.network.dto.FileInitResponseDto
import com.aiwazian.messenger.network.dto.PinChatsRequestDto
import com.aiwazian.messenger.network.dto.TextMessageRequestDto
import com.aiwazian.messenger.socket.OnlineUsersTracker
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
import kotlinx.coroutines.flow.onEach
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
    private val socket: WebSocketClient,
    private val onlineUsersTracker: OnlineUsersTracker
) {
    
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllChats(): Flow<List<Chat>> = userRepository.getMe().flatMapLatest { me ->
        val myId = me.id
        chatDao.getAllChatsFlow(myId).flatMapLatest { chatEntities ->
            if (chatEntities.isEmpty()) return@flatMapLatest flowOf(emptyList())
            
            val flows = chatEntities.map { chatEntity ->
                val lastMessageFlow = if (ChatType.fromId(chatEntity.chatId) == ChatType.PRIVATE) {
                    messageDao.getChatLastMessageFlow(myId, chatEntity.chatId)
                } else {
                    messageDao.getChatLastMessageFlow(chatEntity.chatId)
                }
                
                combine(
                    lastMessageFlow,
                    resolveChatInfoFlow(chatEntity, myId)
                ) { messageWithAttachments, info ->
                    if (info == null) return@combine null
                    val name = info.first
                    val avatarUri = info.second
                    
                    val lastMessage = messageWithAttachments?.let {
                        val attachments = it.attachments.map { att -> att.toDomain() }
                        it.message.toDomain(attachments)
                    }
                    
                    chatEntity.toDomain(name, avatarUri, lastMessage)
                }
            }
            
            combine(flows) { chatsArray ->
                chatsArray.filterNotNull().sortedWith(
                    compareByDescending<Chat> { it.isPinned }
                        .thenByDescending { it.lastMessage?.sendTime ?: 0L }
                )
            }
        }
    }
    
    private fun resolveChatInfoFlow(
        chatEntity: ChatEntity,
        myId: Long
    ): Flow<Pair<UiText, Uri?>?> {
        val chatId = chatEntity.chatId
        return when (ChatType.fromId(chatId)) {
            ChatType.PRIVATE -> userRepository.getByIdOrNull(chatId)
                .fetchIfMissing { userRepository.fetchById(chatId) }
                .map { user ->
                    when {
                        user == null -> null
                        chatId == myId -> Pair(
                            UiText.StringResource(R.string.saved_messages),
                            user.avatars.firstOrNull()?.uri
                        )
                        
                        else -> Pair(
                            UiText.DynamicString("${user.firstName} ${user.lastName.orEmpty()}".trim()),
                            user.avatars.firstOrNull()?.uri
                        )
                    }
                }
            
            ChatType.GROUP -> groupRepository.getByIdOrNull(chatId)
                .fetchIfMissing { groupRepository.fetchById(chatId) }
                .map { group ->
                    group?.let {
                        Pair(
                            UiText.DynamicString(it.name),
                            it.avatars.firstOrNull()?.uri
                        )
                    }
                }
            
            ChatType.CHANNEL -> channelRepository.getByIdOrNull(chatId)
                .fetchIfMissing { channelRepository.fetchById(chatId) }
                .map { channel ->
                    channel?.let {
                        Pair(UiText.DynamicString(it.name), it.avatars.firstOrNull()?.uri)
                    }
                }
            
            else -> flowOf(Pair(UiText.DynamicString(""), null))
        }
    }
    
    private fun <T> Flow<T?>.fetchIfMissing(fetch: suspend () -> Unit): Flow<T?> {
        var fetchTriggered = false
        return onEach { value ->
            if (value == null && !fetchTriggered) {
                fetchTriggered = true
                fetch()
            }
        }
    }
    
    suspend fun refreshChats() {
        try {
            val response = chatApi.getAllChats()
            if (response.isSuccessful) {
                val myId = userRepository.getMe().first().id
                val dtos = response.body().orEmpty()
                val chatIds = dtos.map { it.id }
                
                chatDao.deleteChatsNotIn(myId, chatIds)
                messageDao.deleteMessagesNotInChatIds(chatIds)
                
                chatDao.upsertChats(dtos.map { it.toEntity(myId) })
                
                userRepository.fetchMe()
                
                dtos.forEach { chatDto ->
                    chatDto.lastMessage?.let { lastMessageDto ->
                        saveMessagesToDb(listOf(lastMessageDto.toDomain()))
                    }
                    
                    when (ChatType.fromId(chatDto.id)) {
                        ChatType.PRIVATE -> userRepository.fetchById(chatDto.id)
                        ChatType.GROUP -> groupRepository.fetchById(chatDto.id)
                        ChatType.CHANNEL -> channelRepository.fetchById(chatDto.id)
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
    
    suspend fun getMessages(
        chatId: Long,
        limit: Int? = null,
        offset: Int? = null
    ): Result<List<Message>> {
        return try {
            val response = messageApi.getMessages(chatId, limit, offset)
            if (response.isSuccessful) {
                val dtos = response.body().orEmpty()
                val messages = dtos.map { messageDto ->
                    messageDto.toDomain()
                }
                
                if (messages.isNotEmpty()) {
                    val maxTime = messages.maxOf { it.sendTime }
                    val minTime = messages.minOf { it.sendTime }
                    val receivedIds = messages.map { it.id }
                    val userId = userRepository.getMe().first().id
                    messageDao.deleteMessagesInRangeExcluding(
                        userId,
                        chatId,
                        minTime,
                        maxTime,
                        receivedIds
                    )
                }
                
                saveMessagesToDb(messages)
                Result.success(messages)
            } else {
                Log.e(
                    "ChatRepository",
                    "Failed to get messages for chat $chatId: ${response.message()}"
                )
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting messages for chat $chatId", e)
            Result.failure(e)
        }
    }
    
    fun getLastMessageFlow(chatId: Long): Flow<Message?> {
        return messageDao.getChatLastMessageFlow(chatId).map { messageWithAttachments ->
            messageWithAttachments?.let {
                val attachments = it.attachments.map { att -> att.toDomain() }
                it.message.toDomain(attachments)
            }
        }
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getById(chatId: Long): Flow<Chat?> {
        return userRepository.getMe().flatMapLatest { me ->
            chatDao.getChatByIdFlow(me.id, chatId).flatMapLatest { chatEntity ->
                if (chatEntity == null) return@flatMapLatest flowOf(null)
                resolveChatInfoFlow(chatEntity, me.id).map { info ->
                    if (info == null) return@map null
                    chatEntity.toDomain(info.first, info.second, null)
                }
            }
        }
    }
    
    suspend fun fetchChatByIdFromServer(chatId: Long) {
        try {
            val response = chatApi.getChatById(chatId)
            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    val myId = userRepository.getMe().first().id
                    val chatEntity = dto.toEntity(myId)
                    chatDao.upsertChats(listOf(chatEntity))
                }
            } else {
                Log.e("ChatRepository", "Failed to fetch chat $chatId: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error fetching chat $chatId", e)
        }
    }
    
    suspend fun sendMessage(
        chatId: Long,
        message: String,
        tempId: Long = -System.currentTimeMillis()
    ): Result<Message> {
        val senderId = if (ChatType.fromId(chatId) == ChatType.CHANNEL) chatId
        else userRepository.getMe().first().id
        
        val localMessage = Message(
            id = tempId,
            senderId = senderId,
            chatId = chatId,
            text = message,
            sendTime = System.currentTimeMillis(),
            isRead = false,
            status = MessageStatus.SENDING,
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
                if (sentMessage == null) {
                    Result.failure(Exception("Empty body"))
                } else {
                    updateMessageId(tempId, sentMessage.id)
                    updateMessageStatus(sentMessage.id, MessageStatus.SENT)
                    Result.success(sentMessage)
                }
            } else {
                Log.e("ChatRepository", "Failed to send message: ${response.message()}")
                updateMessageStatus(tempId, MessageStatus.ERROR)
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending message", e)
            updateMessageStatus(tempId, MessageStatus.ERROR)
            Result.failure(e)
        }
    }
    
    suspend fun markMessageAsRead(chatId: Long, messageId: Long) {
        val messageWithAttachments = messageDao.getMessageById(messageId)
        if (messageWithAttachments != null) {
            val msg = messageWithAttachments.message
            messageDao.updateReadStatusBySenderUpTo(
                chatId = msg.chatId,
                senderId = msg.senderId,
                upToSendTime = msg.sendTime,
                isRead = true
            )
        }
    }
    
    suspend fun markReadBySender(chatId: Long, senderId: Long, sendTime: Long) {
        messageDao.updateReadStatusBySenderUpTo(
            chatId = chatId,
            senderId = senderId,
            upToSendTime = sendTime,
            isRead = true
        )
    }
    
    suspend fun saveLocalMessage(message: Message) {
        val myId = userRepository.getMe().first().id
        val targetChatId = if (message.chatId == myId) message.senderId else message.chatId
        
        val existingChat = chatDao.getChatById(myId, targetChatId)
        if (existingChat == null) {
            fetchChatByIdFromServer(targetChatId)
        }
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
                        path = attachment.localUri?.toString(),
                        status = attachment.status
                    )
                    fileRepository.upsert(newFile)
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
    
    suspend fun updateMessageStatus(id: Long, status: MessageStatus) {
        messageDao.updateMessageStatus(id, status)
    }
    
    suspend fun getDownloadUrl(chatId: Long, messageId: Long, fileId: String): Result<String> {
        return try {
            val response = messageApi.getFileDownloadUrl(chatId, messageId, fileId)
            if (response.isSuccessful) {
                val downloadUrl = response.body()
                if (downloadUrl != null) {
                    Result.success(downloadUrl.downloadUrl)
                } else if (response.code() == 404) {
                    deleteLocalMessage(messageId)
                    Result.failure(Exception("Message not found"))
                } else {
                    Result.failure(Exception("Empty download url"))
                }
            } else {
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting download url", e)
            Result.failure(e)
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
        if (messageId < 0) {
            deleteLocalMessage(messageId)
            return Result.success(Unit)
        }
        return try {
            val response = messageApi.deleteMessage(
                chatId,
                messageId,
                DeleteMessageRequestDto(deleteForRecipient)
            )
            if (response.isSuccessful) {
                deleteLocalMessage(messageId)
                Result.success(Unit)
            } else if (response.code() == 404) {
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
    
    suspend fun editMessage(
        chatId: Long,
        messageId: Long,
        text: String
    ): Result<Message> {
        return try {
            val request = EditMessageRequestDto(text = text)
            val response =
                messageApi.editTextMessage(chatId, messageId, request, socket.socketId.orEmpty())
            if (response.isSuccessful) {
                val editedMessage = response.body()?.toDomain()
                if (editedMessage != null) {
                    messageDao.updateMessageTextAndEditedAt(messageId, text, editedMessage.editedAt)
                    Result.success(editedMessage)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error editing message", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateLocalMessage(
        messageId: Long,
        text: String?,
        editedAt: Long?
    ) {
        if (text != null) {
            messageDao.updateMessageTextAndEditedAt(messageId, text, editedAt)
        }
    }
    
    suspend fun clearLocalHistory(chatId: Long) {
        userRepository.getMe().firstOrNull()?.id?.let { userId ->
            messageDao.clearChatHistory(userId, chatId)
        }
    }
    
    suspend fun deleteChatMessages(chatId: Long, clearForRecipient: Boolean = false): Boolean {
        return try {
            val response =
                messageApi.clearHistory(chatId, ClearHistoryRequestDto(clearForRecipient))
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
        val myId = userRepository.getMe().first().id
        chatDao.deleteChat(myId, chatId)
        clearLocalHistory(chatId)
    }
    
    suspend fun deleteChat(chatId: Long, deleteForRecipient: Boolean = false): Boolean {
        return try {
            val myId = userRepository.getMe().first().id
            val response = chatApi.deleteChat(chatId, DeleteChatRequestDto(deleteForRecipient))
            chatDao.deleteChat(myId, chatId)
            clearLocalHistory(chatId)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error deleting chat $chatId", e)
            false
        }
    }
    
    suspend fun pinChats(chatIds: List<Long>): Boolean {
        return try {
            val myId = userRepository.getMe().first().id
            val request = PinChatsRequestDto(chatIds.map { it.toString() })
            val response = chatApi.pinChats(request)
            if (response.isSuccessful) {
                chatDao.updatePinnedStatus(myId, chatIds, true)
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
            val myId = userRepository.getMe().first().id
            val request = PinChatsRequestDto(chatIds.map { it.toString() })
            val response = chatApi.unpinChats(request)
            if (response.isSuccessful) {
                chatDao.updatePinnedStatus(myId, chatIds, false)
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
    
    suspend fun updateChatPinnedStatus(chatIds: List<Long>, isPinned: Boolean) {
        val myId = userRepository.getMe().first().id
        chatDao.updatePinnedStatus(myId, chatIds, isPinned)
    }
    
    suspend fun refreshOnlineUsers() {
        try {
            val response = chatApi.getOnlineUsers()
            if (response.isSuccessful) {
                val onlineIds = response.body().orEmpty().map { it.toLong() }
                onlineUsersTracker.replaceAll(onlineIds)
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error refreshing online users", e)
        }
    }
}
