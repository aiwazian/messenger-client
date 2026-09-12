package com.aiwazian.messenger.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.aiwazian.messenger.database.entity.CustomEmojiEntity
import com.aiwazian.messenger.database.entity.EmojiPackEntity

@Dao
interface EmojiDao {
    
    @Query("SELECT * FROM emoji_pack WHERE isInstalled = 1 ORDER BY sortOrder ASC")
    suspend fun getInstalledPacks(): List<EmojiPackEntity>
    
    @Query("SELECT * FROM emoji_pack WHERE isOwned = 1 ORDER BY sortOrder ASC")
    suspend fun getOwnedPacks(): List<EmojiPackEntity>
    
    @Query("SELECT * FROM emoji_pack WHERE id = :packId LIMIT 1")
    suspend fun getPack(packId: Long): EmojiPackEntity?
    
    @Query("SELECT * FROM emoji_pack WHERE username = :username LIMIT 1")
    suspend fun getPackByUsername(username: String): EmojiPackEntity?
    
    @Query("SELECT * FROM emoji WHERE packId = :packId ORDER BY sortOrder ASC")
    suspend fun getEmojis(packId: Long): List<CustomEmojiEntity>
    
    @Query("SELECT * FROM emoji WHERE id IN (:emojiIds)")
    suspend fun getEmojisByIds(emojiIds: List<Long>): List<CustomEmojiEntity>
    
    @Upsert
    suspend fun upsertPacks(packs: List<EmojiPackEntity>)
    
    @Upsert
    suspend fun upsertEmojis(emojis: List<CustomEmojiEntity>)
    
    @Query("UPDATE emoji_pack SET isInstalled = 0")
    suspend fun clearInstalled()
    
    @Query("UPDATE emoji_pack SET isInstalled = :isInstalled WHERE id = :packId")
    suspend fun setInstalled(packId: Long, isInstalled: Boolean)
    
    @Query("DELETE FROM emoji WHERE packId = :packId")
    suspend fun deleteEmojis(packId: Long)
    
    @Query("DELETE FROM emoji_pack WHERE id = :packId")
    suspend fun deletePack(packId: Long)
}
