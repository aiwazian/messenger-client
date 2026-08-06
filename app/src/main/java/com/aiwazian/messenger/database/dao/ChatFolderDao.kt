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
    
    @Query("SELECT * FROM chat_folders WHERE userId = :userId ORDER BY sortOrder ASC, id ASC")
    fun getFoldersFlow(userId: Long): Flow<List<ChatFolderEntity>>
    
    @Query("SELECT * FROM chat_folder_chats WHERE userId = :userId ORDER BY sortOrder ASC")
    fun getFolderChatsFlow(userId: Long): Flow<List<ChatFolderChatEntity>>
    
    @Query("SELECT * FROM chat_folders WHERE userId = :userId AND id = :folderId")
    suspend fun getFolder(userId: Long, folderId: Int): ChatFolderEntity?
    
    @Query(
        "SELECT * FROM chat_folder_chats WHERE userId = :userId AND folderId = :folderId " +
                "ORDER BY sortOrder ASC"
    )
    suspend fun getFolderChats(userId: Long, folderId: Int): List<ChatFolderChatEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<ChatFolderEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolderChats(chats: List<ChatFolderChatEntity>)
    
    @Query("DELETE FROM chat_folders WHERE userId = :userId")
    suspend fun deleteAllFolders(userId: Long)
    
    @Query("DELETE FROM chat_folders WHERE userId = :userId AND id = :folderId")
    suspend fun deleteFolder(userId: Long, folderId: Int)
    
    @Query("DELETE FROM chat_folder_chats WHERE userId = :userId AND folderId = :folderId")
    suspend fun deleteFolderChats(userId: Long, folderId: Int)
    
    /**
     * Сервер отдаёт список папок целиком, поэтому ответ замещает папки этого
     * аккаунта целиком: удалённая на другом устройстве папка исчезает сама.
     * Кэш остальных учёток при этом не трогается.
     */
    @Transaction
    suspend fun replaceFolders(
        userId: Long,
        folders: List<ChatFolderEntity>,
        chats: List<ChatFolderChatEntity>
    ) {
        deleteAllFolders(userId)
        insertFolders(folders)
        insertFolderChats(chats)
    }
    
    @Transaction
    suspend fun upsertFolder(folder: ChatFolderEntity, chats: List<ChatFolderChatEntity>) {
        insertFolders(listOf(folder))
        deleteFolderChats(folder.userId, folder.id)
        insertFolderChats(chats)
    }
}
