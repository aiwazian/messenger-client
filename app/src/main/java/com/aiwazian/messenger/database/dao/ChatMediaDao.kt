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

    @Query(
        """
        SELECT * FROM chat_media
        WHERE chatId = :chatId
            AND type = 'FILE'
            AND name NOT LIKE '%.mp3'
            AND name NOT LIKE '%.wav'
            AND name NOT LIKE '%.flac'
            AND name NOT LIKE '%.m4a'
            AND name NOT LIKE '%.aac'
            AND name NOT LIKE '%.wma'
            AND name NOT LIKE '%.amr'
            AND ownerId = (SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)
        ORDER BY id DESC
        LIMIT :limit
        """
    )
    suspend fun getFilesWindow(chatId: Long, limit: Int): List<ChatMediaEntity>

    @Query(
        """
        SELECT * FROM chat_media
        WHERE chatId = :chatId
            AND type = 'FILE'
            AND (
                name LIKE '%.mp3'
                OR name LIKE '%.wav'
                OR name LIKE '%.flac'
                OR name LIKE '%.m4a'
                OR name LIKE '%.aac'
                OR name LIKE '%.wma'
                OR name LIKE '%.amr'
            )
            AND ownerId = (SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)
        ORDER BY id DESC
        LIMIT :limit
        """
    )
    suspend fun getMusicWindow(chatId: Long, limit: Int): List<ChatMediaEntity>

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

    @Transaction
    suspend fun saveFilesWindow(chatId: Long, items: List<ChatMediaEntity>) {
        val ownerId = getCurrentOwnerId() ?: return

        if (items.isEmpty()) {
            clearFiles(chatId)

            return
        }

        upsert(items.map { it.copy(ownerId = ownerId) })
        pruneFilesWindow(
            chatId = chatId,
            fromId = items.minOf { it.id },
            keepIds = items.map { it.id })
    }

    @Transaction
    suspend fun saveMusicWindow(chatId: Long, items: List<ChatMediaEntity>) {
        val ownerId = getCurrentOwnerId() ?: return

        if (items.isEmpty()) {
            clearMusic(chatId)

            return
        }

        upsert(items.map { it.copy(ownerId = ownerId) })
        pruneMusicWindow(
            chatId = chatId,
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
            AND type = 'FILE'
            AND name NOT LIKE '%.mp3'
            AND name NOT LIKE '%.wav'
            AND name NOT LIKE '%.flac'
            AND name NOT LIKE '%.m4a'
            AND name NOT LIKE '%.aac'
            AND name NOT LIKE '%.wma'
            AND name NOT LIKE '%.amr'
            AND ownerId = (SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)
            AND id >= :fromId
            AND id NOT IN (:keepIds)
        """
    )
    suspend fun pruneFilesWindow(
        chatId: Long,
        fromId: Int,
        keepIds: List<Int>
    )

    @Query(
        """
        DELETE FROM chat_media
        WHERE chatId = :chatId
            AND type = 'FILE'
            AND (
                name LIKE '%.mp3'
                OR name LIKE '%.wav'
                OR name LIKE '%.flac'
                OR name LIKE '%.m4a'
                OR name LIKE '%.aac'
                OR name LIKE '%.wma'
                OR name LIKE '%.amr'
            )
            AND ownerId = (SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)
            AND id >= :fromId
            AND id NOT IN (:keepIds)
        """
    )
    suspend fun pruneMusicWindow(
        chatId: Long,
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

    @Query(
        """
        DELETE FROM chat_media
        WHERE chatId = :chatId
            AND type = 'FILE'
            AND name NOT LIKE '%.mp3'
            AND name NOT LIKE '%.wav'
            AND name NOT LIKE '%.flac'
            AND name NOT LIKE '%.m4a'
            AND name NOT LIKE '%.aac'
            AND name NOT LIKE '%.wma'
            AND name NOT LIKE '%.amr'
            AND ownerId = (SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)
        """
    )
    suspend fun clearFiles(chatId: Long)

    @Query(
        """
        DELETE FROM chat_media
        WHERE chatId = :chatId
            AND type = 'FILE'
            AND (
                name LIKE '%.mp3'
                OR name LIKE '%.wav'
                OR name LIKE '%.flac'
                OR name LIKE '%.m4a'
                OR name LIKE '%.aac'
                OR name LIKE '%.wma'
                OR name LIKE '%.amr'
            )
            AND ownerId = (SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1)
        """
    )
    suspend fun clearMusic(chatId: Long)

    @Query("SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1")
    suspend fun getCurrentOwnerId(): Long?
}
