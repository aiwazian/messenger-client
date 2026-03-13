package com.aiwazian.messenger.database.repository

import android.util.Log
import com.aiwazian.messenger.api.RetrofitInstance
import com.aiwazian.messenger.data.ChatInfo
import com.aiwazian.messenger.data.FolderInfo
import com.aiwazian.messenger.database.dao.FolderChatDao
import com.aiwazian.messenger.database.dao.FolderDao
import com.aiwazian.messenger.database.mappers.toEntity
import com.aiwazian.messenger.database.mappers.toChat
import com.aiwazian.messenger.database.mappers.toFolder
import com.aiwazian.messenger.services.FolderService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepository @Inject constructor(
    private val folderService: FolderService,
    private val folderDao: FolderDao,
    private val folderChatDao: FolderChatDao
) {
    
    private val _folders = MutableStateFlow<List<FolderInfo>>(emptyList())
    val folders = _folders.asStateFlow()
    
    suspend fun loadFolders() {
        val localFolderEntities = folderDao.getAll()
        
        val localFolders = localFolderEntities.map { it.toFolder() }
        
        _folders.update {
            localFolders.map { folder ->
                folder.chats = folderChatDao.getAll(folder.id).map { it.toChat() }
                folder
            }
        }
        
        try {
            val folders = folderService.getAll()
            
            if (folders == null) {
                return
            }
            
            val response = RetrofitInstance.api.getUnarchivedChats().body()
            
            val chatFolderInfos = listOf(
                FolderInfo(
                    id = 0,
                    name = "Все чаты",
                    chats = response ?: emptyList()
                )
            ) + folders
            
            _folders.update { chatFolderInfos }
            
            val folderEntities = _folders.value.map { it.toEntity() }
            folderDao.insertAll(folderEntities)
            
            _folders.value.forEach { folder ->
                val chatEntities = folder.chats.map { it.toEntity(folder.id) }
                folderChatDao.insertAll(chatEntities)
            }
        } catch (e: Exception) {
            Log.e(
                "FolderRepository",
                "Ошибка при получении папок с чатами",
                e
            )
        }
    }
    
    fun getFolderChats(folderId: Int): List<ChatInfo> {
        return _folders.value.find { it.id == folderId }?.chats ?: emptyList()
    }
    
    suspend fun saveFolder(folderInfo: FolderInfo) {
        val folderId = folderService.save(folderInfo)
        
        if (folderId == null) {
            return
        }
        
        folderInfo.id = folderId
        
        _folders.update { currentFolders ->
            val existingIndex = currentFolders.indexOfFirst { it.id == folderInfo.id }
            
            if (existingIndex != -1) {
                currentFolders.toMutableList().apply {
                    this[existingIndex] = folderInfo
                }
            } else {
                currentFolders + folderInfo
            }
        }
    }
    
    suspend fun remove(folderId: Int): Boolean {
        val folder = _folders.value.find { it.id == folderId }?.toEntity()
        
        if (folder == null) {
            return false
        }
        
        _folders.update { it.filter { it -> it.id != folderId } }
        
        folderDao.delete(folder)
        
        return folderService.remove(folderId)
    }
}