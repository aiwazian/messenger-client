package com.aiwazian.messenger.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.aiwazian.messenger.database.entity.StickerEntity
import com.aiwazian.messenger.database.entity.StickerPackEntity

@Dao
interface StickerDao {
    
    @Query("SELECT * FROM sticker_pack WHERE isInstalled = 1 ORDER BY sortOrder ASC")
    suspend fun getInstalledPacks(): List<StickerPackEntity>
    
    @Query("SELECT * FROM sticker_pack WHERE isOwned = 1 ORDER BY sortOrder ASC")
    suspend fun getOwnedPacks(): List<StickerPackEntity>
    
    @Query("SELECT * FROM sticker_pack WHERE id = :packId LIMIT 1")
    suspend fun getPack(packId: Long): StickerPackEntity?
    
    @Query("SELECT * FROM sticker_pack WHERE username = :username LIMIT 1")
    suspend fun getPackByUsername(username: String): StickerPackEntity?
    
    @Query("SELECT * FROM sticker WHERE packId = :packId ORDER BY sortOrder ASC")
    suspend fun getStickers(packId: Long): List<StickerEntity>
    
    @Upsert
    suspend fun upsertPacks(packs: List<StickerPackEntity>)
    
    @Upsert
    suspend fun upsertStickers(stickers: List<StickerEntity>)
    
    @Query("UPDATE sticker_pack SET isInstalled = 0")
    suspend fun clearInstalled()
    
    @Query("UPDATE sticker_pack SET isInstalled = :isInstalled WHERE id = :packId")
    suspend fun setInstalled(packId: Long, isInstalled: Boolean)
    
    @Query("DELETE FROM sticker WHERE packId = :packId")
    suspend fun deleteStickers(packId: Long)
    
    @Query("DELETE FROM sticker_pack WHERE id = :packId")
    suspend fun deletePack(packId: Long)
}
