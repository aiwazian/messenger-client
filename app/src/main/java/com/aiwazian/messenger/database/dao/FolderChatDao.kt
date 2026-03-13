package com.aiwazian.messenger.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aiwazian.messenger.database.entity.FolderChatEntity
import com.aiwazian.messenger.database.entity.MessageEntity

@Dao
interface FolderChatDao {
    @Query("SELECT * FROM folderChat WHERE folderId = :id")
    suspend fun getAll(id: Int): List<FolderChatEntity>
    
    @Query("SELECT * FROM folderChat WHERE id = :id")
    suspend fun get(id: Long): FolderChatEntity?
    
    @Query("SELECT * FROM message WHERE chatId = :id")
    suspend fun getMessages(id: Long): List<MessageEntity>
    
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAll(chatEntities: List<FolderChatEntity>)
    
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(chatEntity: FolderChatEntity)
    
    @Delete
    suspend fun delete(folderChatEntity: FolderChatEntity)
    
    @Query("DELETE FROM folderChat WHERE id = :id")
    suspend fun deleteById(id: Long)
}