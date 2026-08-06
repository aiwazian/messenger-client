/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.database.dao.ChatFolderDao
import com.aiwazian.messenger.database.entity.ChatFolderChatEntity
import com.aiwazian.messenger.domain.ChatFolder
import com.aiwazian.messenger.enums.ChatFolderCategory
import com.aiwazian.messenger.mappers.toChatEntities
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.mappers.toEntity
import com.aiwazian.messenger.network.api.ChatFolderApi
import com.aiwazian.messenger.network.dto.CreateChatFolderRequestDto
import com.aiwazian.messenger.network.dto.PinFolderChatsRequestDto
import com.aiwazian.messenger.network.dto.ReorderChatFoldersRequestDto
import com.aiwazian.messenger.network.dto.UpdateChatFolderRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ChatFolderRepository"

/**
 * Единый источник правды по папкам — Room. Экраны подписываются на локальный
 * Flow и рисуются сразу при запуске, а ответ сервера только обновляет таблицы.
 */
@Singleton
class ChatFolderRepository @Inject constructor(
    private val chatFolderApi: ChatFolderApi,
    private val chatFolderDao: ChatFolderDao
) {
    
    fun getFolders(): Flow<List<ChatFolder>> {
        return combine(
            chatFolderDao.getFoldersFlow(),
            chatFolderDao.getFolderChatsFlow()
        ) { folders, chats ->
            val chatsByFolder = chats.groupBy { it.folderId }
            folders.map { folder -> folder.toDomain(chatsByFolder[folder.id].orEmpty()) }
        }
    }
    
    suspend fun getFolder(folderId: Int): ChatFolder? {
        val folder = chatFolderDao.getFolder(folderId) ?: return null
        return folder.toDomain(chatFolderDao.getFolderChats(folderId))
    }
    
    suspend fun refreshFolders() {
        try {
            val response = chatFolderApi.getFolders()
            if (response.isSuccessful) {
                val folders = response.body().orEmpty().map { it.toDomain() }
                chatFolderDao.replaceFolders(
                    folders = folders.map { it.toEntity() },
                    chats = folders.flatMap { it.toChatEntities() }
                )
            } else {
                Log.e(TAG, "Failed to get chat folders: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting chat folders", e)
        }
    }
    
    suspend fun createFolder(
        name: String,
        chatIds: List<Long>,
        categories: List<ChatFolderCategory>
    ): Result<ChatFolder> {
        return try {
            val request = CreateChatFolderRequestDto(
                name = name,
                chatIds = chatIds.map { it.toString() },
                categories = categories
            )
            val response = chatFolderApi.createFolder(request)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val folder = body.toDomain()
                chatFolderDao.upsertFolder(folder.toEntity(), folder.toChatEntities())
                Result.success(folder)
            } else {
                Log.e(TAG, "Failed to create chat folder: ${response.message()}")
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating chat folder", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateFolder(
        folderId: Int,
        name: String? = null,
        chatIds: List<Long>? = null,
        categories: List<ChatFolderCategory>? = null
    ): Result<ChatFolder> {
        return try {
            val request = UpdateChatFolderRequestDto(
                name = name,
                chatIds = chatIds?.map { it.toString() },
                categories = categories
            )
            val response = chatFolderApi.updateFolder(folderId, request)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val folder = body.toDomain()
                chatFolderDao.upsertFolder(folder.toEntity(), folder.toChatEntities())
                Result.success(folder)
            } else {
                Log.e(TAG, "Failed to update chat folder: ${response.message()}")
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating chat folder", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteFolder(folderId: Int): Boolean {
        return try {
            val response = chatFolderApi.deleteFolder(folderId)
            if (response.isSuccessful) {
                chatFolderDao.deleteFolder(folderId)
                true
            } else {
                Log.e(TAG, "Failed to delete chat folder: ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting chat folder", e)
            false
        }
    }
    
    suspend fun pinChats(folderId: Int, chatIds: List<Long>): Boolean {
        return setChatsPinned(folderId, chatIds, true)
    }
    
    suspend fun unpinChats(folderId: Int, chatIds: List<Long>): Boolean {
        return setChatsPinned(folderId, chatIds, false)
    }
    
    /** Порядок вкладок: позиция id в списке становится новым порядком папки. */
    suspend fun reorderFolders(folderIds: List<Int>): Boolean {
        if (folderIds.isEmpty()) return true
        return try {
            val response = chatFolderApi.reorderFolders(ReorderChatFoldersRequestDto(folderIds))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val folders = body.map { it.toDomain() }
                chatFolderDao.replaceFolders(
                    folders = folders.map { it.toEntity() },
                    chats = folders.flatMap { it.toChatEntities() }
                )
                true
            } else {
                Log.e(TAG, "Failed to reorder chat folders: ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reordering chat folders", e)
            false
        }
    }
    
    private suspend fun setChatsPinned(
        folderId: Int,
        chatIds: List<Long>,
        isPinned: Boolean
    ): Boolean {
        if (chatIds.isEmpty()) return true
        
        applyPinsLocally(folderId, chatIds, isPinned)
        
        return try {
            val request = PinFolderChatsRequestDto(chatIds.map { it.toString() })
            val response = if (isPinned) {
                chatFolderApi.pinChats(folderId, request)
            } else {
                chatFolderApi.unpinChats(folderId, request)
            }
            
            if (response.isSuccessful) {
                true
            } else {
                Log.e(TAG, "Failed to pin folder chats: ${response.message()}")
                refreshFolders()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pinning folder chats", e)
            refreshFolders()
            false
        }
    }
    
    /**
     * Чат, попавший в папку через категорию, своей строки не имеет: чтобы
     * закрепление было видно до ответа сервера, строка заводится локально ровно
     * так же, как её заведёт сервер.
     */
    private suspend fun applyPinsLocally(
        folderId: Int,
        chatIds: List<Long>,
        isPinned: Boolean
    ) {
        val existing = chatFolderDao.getFolderChats(folderId)
        val existingIds = existing.map { it.chatId }.toSet()
        
        val updated = existing.map { chat ->
            if (chat.chatId in chatIds) chat.copy(isPinned = isPinned) else chat
        }
        
        val created = chatIds.filterNot { it in existingIds }.mapIndexed { index, chatId ->
            ChatFolderChatEntity(
                folderId = folderId,
                chatId = chatId,
                isIncluded = false,
                isPinned = isPinned,
                sortOrder = existing.size + index
            )
        }
        
        chatFolderDao.insertFolderChats(updated + created)
    }
}
