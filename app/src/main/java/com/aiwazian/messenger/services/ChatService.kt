package com.aiwazian.messenger.services

import com.aiwazian.messenger.api.RetrofitInstance
import com.aiwazian.messenger.data.ChatInfo
import com.aiwazian.messenger.data.Message
import okhttp3.MultipartBody
import javax.inject.Inject

class ChatService @Inject constructor() {
    
    suspend fun sendMessage(chatId: Long, message: Message): Message? {
        val response = RetrofitInstance.api.sendMessage(chatId, message)
        return if (response.isSuccessful) response.body() else null
    }
    
    suspend fun sendDocument(fileUri: MultipartBody.Part, chatId: Long): Message? {
        val response = RetrofitInstance.api.sendDocument(fileUri, chatId)
        return if (response.isSuccessful) response.body() else null
    }
    
    suspend fun getChatInfo(chatId: Long): ChatInfo? {
        val response = RetrofitInstance.api.getChatInfo(chatId)
        return response.body()
    }
    
    suspend fun getAllChatsWithOtherUser(): List<ChatInfo>? {
        val response = RetrofitInstance.api.getAllChatsWithOtherUser()
        return response.body()
    }
    
    suspend fun makeAsReadMessage(
        chatId: Long,
        messageId: Int
    ): Boolean {
        val response = RetrofitInstance.api.makeAsReadMessage(
            chatId,
            messageId
        )
        
        return response.isSuccessful
    }
    
    suspend fun getChatLastMessage(chatId: Long): Message? {
        val response = RetrofitInstance.api.getChatLastMessage(chatId)
        return response.body()
    }
    
    suspend fun getChatMessages(chatId: Long): List<Message>? {
        val response = RetrofitInstance.api.getMessagesBetweenUsers(chatId)
        return response.body()
    }
    
    suspend fun archiveChat(chatId: Long): Boolean {
        val response = RetrofitInstance.api.archiveChat(chatId)
        return response.isSuccessful
    }
    
    suspend fun unarchiveChat(chatId: Long): Boolean {
        val response = RetrofitInstance.api.unarchiveChat(chatId)
        return response.isSuccessful
    }
    
    suspend fun pin(
        chatId: Long,
        folderId: Int
    ): Boolean {
        val response = if (folderId == 0) {
            RetrofitInstance.api.pinChat(chatId)
        } else {
            RetrofitInstance.api.pinChatInFolder(
                folderId,
                chatId
            )
        }
        
        return response.isSuccessful
    }
    
    suspend fun unpin(
        chatId: Long,
        folderId: Int
    ): Boolean {
        val response = if (folderId == 0) {
            RetrofitInstance.api.unpinChat(chatId)
        } else {
            RetrofitInstance.api.unpinChatInFolder(
                folderId,
                chatId
            )
        }
        
        return response.isSuccessful
    }
    
    suspend fun deleteMessage(
        chatId: Long,
        messageId: Int,
        deleteForAll: Boolean
    ): Boolean {
        val response = RetrofitInstance.api.deleteMessage(
            chatId,
            messageId,
            deleteForAll
        )
        
        return response.isSuccessful
    }
    
    suspend fun deleteChat(
        chatId: Long,
        deleteForReceiver: Boolean
    ): Boolean {
        val response = RetrofitInstance.api.deleteChat(
            chatId,
            deleteForReceiver
        )
        
        return response.isSuccessful
    }
    
    suspend fun deleteChatMessages(
        chatId: Long,
        deleteForReceiver: Boolean
    ): Boolean {
        val response = RetrofitInstance.api.deleteChatMessages(
            chatId,
            deleteForReceiver
        )
        
        return response.isSuccessful
    }
}