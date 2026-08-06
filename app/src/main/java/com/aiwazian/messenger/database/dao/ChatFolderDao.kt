/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.aiwazian.messenger.database.entity.ChatFolderChatEntity
import com.aiwazian.messenger.database.entity.ChatFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatFolderDao {
    
    @Query("SELECT * FROM chat_folders ORDER BY sortOrder ASC, id ASC")
    fun getFoldersFlow(): Flow<List<ChatFolderEntity>>
    
    @Query("SELECT * FROM chat_folder_chats ORDER BY sortOrder ASC")
    fun getFolderChatsFlow(): Flow<List<ChatFolderChatEntity>>
    
    @Query("SELECT * FROM chat_folders WHERE id = :folderId")
    suspend fun getFolder(folderId: Int): ChatFolderEntity?
    
    @Query("SELECT * FROM chat_folder_chats WHERE folderId = :folderId ORDER BY sortOrder ASC")
    suspend fun getFolderChats(folderId: Int): List<ChatFolderChatEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<ChatFolderEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolderChats(chats: List<ChatFolderChatEntity>)
    
    @Query("DELETE FROM chat_folders")
    suspend fun deleteAllFolders()
    
    @Query("DELETE FROM chat_folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: Int)
    
    @Query("DELETE FROM chat_folder_chats WHERE folderId = :folderId")
    suspend fun deleteFolderChats(folderId: Int)
    
    /**
     * Сервер отдаёт список папок целиком, поэтому ответ замещает содержимое
     * таблиц целиком: удалённая на другом устройстве папка исчезает сама.
     */
    @Transaction
    suspend fun replaceFolders(
        folders: List<ChatFolderEntity>,
        chats: List<ChatFolderChatEntity>
    ) {
        deleteAllFolders()
        insertFolders(folders)
        insertFolderChats(chats)
    }
    
    @Transaction
    suspend fun upsertFolder(folder: ChatFolderEntity, chats: List<ChatFolderChatEntity>) {
        insertFolders(listOf(folder))
        deleteFolderChats(folder.id)
        insertFolderChats(chats)
    }
}
