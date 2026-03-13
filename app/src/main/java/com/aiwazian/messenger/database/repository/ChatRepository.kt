package com.aiwazian.messenger.database.repository

import android.util.Log
import com.aiwazian.messenger.data.Attachment
import com.aiwazian.messenger.data.ChatInfo
import com.aiwazian.messenger.data.Message
import com.aiwazian.messenger.database.dao.ChatDao
import com.aiwazian.messenger.database.dao.FolderChatDao
import com.aiwazian.messenger.database.mappers.toChat
import com.aiwazian.messenger.database.mappers.toEntity
import com.aiwazian.messenger.database.mappers.toMessage
import com.aiwazian.messenger.database.mappers.toModel
import com.aiwazian.messenger.services.ChatService
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val chatService: ChatService,
    private val folderChatDao: FolderChatDao,
    private val chatDao: ChatDao
) {
    
    suspend fun get(id: Long): ChatInfo? {
        try {
            val chat = chatService.getChatInfo(id)
            
            if (chat != null) {
                return chat
            }
        } catch (e: Exception) {
            Log.e(
                "ChatRepository",
                "Ошибка при получении информации о чате",
                e
            )
        }
        
        val localChat = folderChatDao.get(id)
        
        return localChat?.toChat()
    }
    
    suspend fun getMessages(id: Long): List<Message> {
        try {
            val messages = chatService.getChatMessages(id)
            
            if (messages != null) {
                return messages
            }
        } catch (e: Exception) {
            Log.e(
                "ChatRepository",
                "Ошибка при получении сообщений",
                e
            )
        }
        
        val localMessages = folderChatDao.getMessages(id)
        
        if (localMessages.isEmpty()) {
            return emptyList()
        }
        
        return localMessages.map { it.toMessage() }
    }
    
    suspend fun getLastMessage(id: Long): Message? {
        return chatService.getChatLastMessage(id)
    }
    
    suspend fun sendMessage(chatId: Long, message: Message): Message? {
        return chatService.sendMessage(chatId, message)
    }
    
    suspend fun saveAttachment(attachment: Attachment) {
        chatDao.save(attachment.toEntity())
    }
    
    suspend fun getAttachment(id: Int): Attachment {
        return chatDao.get(id).toModel()
    }
    
    suspend fun makeAsRead(
        chatId: Long,
        messageId: Int
    ): Boolean {
        return chatService.makeAsReadMessage(
            chatId,
            messageId
        )
    }
    
    suspend fun deleteMessage(
        chatId: Long,
        messageId: Int,
        deleteForAll: Boolean
    ): Boolean {
        try {
            return chatService.deleteMessage(
                chatId,
                messageId,
                deleteForAll
            )
        } catch (e: Exception) {
            Log.e(
                "ChatRepository",
                "Ошибка при удалени сообщения",
                e
            )
            
            return false
        }
    }
    
    suspend fun deleteChat(
        chatId: Long
    ) {
        try {
            folderChatDao.deleteById(chatId)
            
        } catch (e: Exception) {
            Log.e(
                "ChatRepository",
                "Ошибка при удалении чата",
                e
            )
        }
    }
    
    suspend fun deleteChatMessages(
        chatId: Long,
        deleteForReceiver: Boolean
    ): Boolean {
        return chatService.deleteChatMessages(
            chatId,
            deleteForReceiver
        )
    }
    
    suspend fun pin(
        chatId: Long,
        folderId: Int
    ): Boolean {
        return chatService.pin(
            chatId,
            folderId
        )
    }
    
    suspend fun unpin(
        chatId: Long,
        folderId: Int
    ): Boolean {
        return chatService.unpin(
            chatId,
            folderId
        )
    }
    
    suspend fun archive(id: Long): Boolean {
        return chatService.archiveChat(id)
    }
    
    suspend fun unarchive(id: Long): Boolean {
        return chatService.unarchiveChat(id)
    }
}