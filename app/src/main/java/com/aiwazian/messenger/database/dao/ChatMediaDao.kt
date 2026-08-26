/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.aiwazian.messenger.database.entity.ChatMediaCountsEntity
import com.aiwazian.messenger.database.entity.ChatMediaEntity
import com.aiwazian.messenger.database.entity.VoiceDurationEntity

/**
 * Кэш галереи чата.
 *
 * Типы вложений передаются строками: в базе перечисление лежит именем, и
 * сравнение со списком имён не зависит от того, как Room решит конвертировать
 * список параметров в запросе.
 *
 * Владелец везде берётся из текущего аккаунта тем же подзапросом, что и у
 * сообщений: так вызывающему коду не нужно таскать его за собой через все
 * слои.
 */
@Dao
interface ChatMediaDao {
    
    @Query(
        """
        SELECT * FROM chat_media
        WHERE chatId = :chatId
            AND type IN (:types)
            AND ownerId = (SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)
        ORDER BY id DESC
        LIMIT :limit
        """
    )
    suspend fun getByTypes(
        chatId: Long,
        types: List<String>,
        limit: Int
    ): List<ChatMediaEntity>
    
    /**
     * Кладёт свежее окно вкладки вместо старого.
     *
     * Лишние строки в пределах окна убираются, а не остаются до следующего
     * запуска: удалённое вложение иначе всплывало бы первым кадром при
     * каждом открытии. Строки глубже окна не трогаются: их всё равно никто
     * не читает, а лишнее удаление — лишняя запись в базу.
     */
    @Transaction
    suspend fun saveWindow(
        chatId: Long,
        types: List<String>,
        items: List<ChatMediaEntity>
    ) {
        val ownerId = getCurrentOwnerId() ?: return
        
        if (items.isEmpty()) {
            clear(chatId, types)
            
            return
        }
        
        upsert(items.map { it.copy(ownerId = ownerId) })
        pruneWindow(
            chatId = chatId,
            types = types,
            fromId = items.minOf { it.id },
            keepIds = items.map { it.id })
    }
    
    @Query(
        """
        SELECT * FROM chat_media_counts
        WHERE chatId = :chatId
            AND ownerId = (SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)
        """
    )
    suspend fun getCounts(chatId: Long): ChatMediaCountsEntity?
    
    @Transaction
    suspend fun saveCounts(counts: ChatMediaCountsEntity) {
        val ownerId = getCurrentOwnerId() ?: return
        
        upsertCounts(counts.copy(ownerId = ownerId))
    }
    
    @Query("SELECT * FROM voice_duration WHERE fileId IN (:fileIds)")
    suspend fun getVoiceDurations(fileIds: List<String>): List<VoiceDurationEntity>
    
    @Upsert
    suspend fun upsertVoiceDuration(duration: VoiceDurationEntity)
    
    @Upsert
    suspend fun upsert(items: List<ChatMediaEntity>)
    
    @Upsert
    suspend fun upsertCounts(counts: ChatMediaCountsEntity)
    
    @Query(
        """
        DELETE FROM chat_media
        WHERE chatId = :chatId
            AND type IN (:types)
            AND ownerId = (SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)
            AND id >= :fromId
            AND id NOT IN (:keepIds)
        """
    )
    suspend fun pruneWindow(
        chatId: Long,
        types: List<String>,
        fromId: Int,
        keepIds: List<Int>
    )
    
    @Query(
        """
        DELETE FROM chat_media
        WHERE chatId = :chatId
            AND type IN (:types)
            AND ownerId = (SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)
        """
    )
    suspend fun clear(chatId: Long, types: List<String>)
    
    @Query("SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1")
    suspend fun getCurrentOwnerId(): Long?
}
