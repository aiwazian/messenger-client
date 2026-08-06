/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.repository

import android.util.Log
import com.aiwazian.messenger.domain.ChatFolder
import com.aiwazian.messenger.enums.ChatFolderCategory
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.network.api.ChatFolderApi
import com.aiwazian.messenger.network.dto.CreateChatFolderRequestDto
import com.aiwazian.messenger.network.dto.PinFolderChatsRequestDto
import com.aiwazian.messenger.network.dto.ReorderChatFoldersRequestDto
import com.aiwazian.messenger.network.dto.UpdateChatFolderRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Папки с чатами живут в памяти процесса, а не в Room: список короткий и
 * запрашивается одним запросом при подключении сокета.
 */
@Singleton
class ChatFolderRepository @Inject constructor(
    private val chatFolderApi: ChatFolderApi
) {
    
    private val _folders = MutableStateFlow<List<ChatFolder>>(emptyList())
    
    fun getFolders(): Flow<List<ChatFolder>> = _folders.asStateFlow()
    
    suspend fun refreshFolders() {
        try {
            val response = chatFolderApi.getFolders()
            if (response.isSuccessful) {
                _folders.value = response.body().orEmpty().map { it.toDomain() }.sortedBy { it.sortOrder }
            } else {
                Log.e("ChatFolderRepository", "Failed to get chat folders: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("ChatFolderRepository", "Error getting chat folders", e)
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
                _folders.value = (_folders.value + folder).sortedBy { it.sortOrder }
                Result.success(folder)
            } else {
                Log.e("ChatFolderRepository", "Failed to create chat folder: ${response.message()}")
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e("ChatFolderRepository", "Error creating chat folder", e)
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
                _folders.value = _folders.value
                    .map { if (it.id == folder.id) folder else it }
                    .sortedBy { it.sortOrder }
                Result.success(folder)
            } else {
                Log.e("ChatFolderRepository", "Failed to update chat folder: ${response.message()}")
                Result.failure(Exception("Unsuccessful request ${response.errorBody()}"))
            }
        } catch (e: Exception) {
            Log.e("ChatFolderRepository", "Error updating chat folder", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteFolder(folderId: Int): Boolean {
        return try {
            val response = chatFolderApi.deleteFolder(folderId)
            if (response.isSuccessful) {
                _folders.value = _folders.value.filterNot { it.id == folderId }
                true
            } else {
                Log.e("ChatFolderRepository", "Failed to delete chat folder: ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e("ChatFolderRepository", "Error deleting chat folder", e)
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
                _folders.value = body.map { it.toDomain() }.sortedBy { it.sortOrder }
                true
            } else {
                Log.e("ChatFolderRepository", "Failed to reorder chat folders: ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e("ChatFolderRepository", "Error reordering chat folders", e)
            false
        }
    }
    
    private suspend fun setChatsPinned(
        folderId: Int,
        chatIds: List<Long>,
        isPinned: Boolean
    ): Boolean {
        if (chatIds.isEmpty()) return true
        return try {
            val request = PinFolderChatsRequestDto(chatIds.map { it.toString() })
            val response = if (isPinned) {
                chatFolderApi.pinChats(folderId, request)
            } else {
                chatFolderApi.unpinChats(folderId, request)
            }
            
            if (response.isSuccessful) {
                _folders.value = _folders.value.map { folder ->
                    if (folder.id != folderId) return@map folder
                    folder.copy(chats = folder.chats.map { chat ->
                        if (chat.chatId in chatIds) chat.copy(isPinned = isPinned) else chat
                    })
                }
                true
            } else {
                Log.e("ChatFolderRepository", "Failed to pin folder chats: ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e("ChatFolderRepository", "Error pinning folder chats", e)
            false
        }
    }
}
