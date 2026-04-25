/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.aiwazian.messenger.database.entity.AttachmentEntity
import com.aiwazian.messenger.enums.DownloadStatus

@Dao
interface AttachmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttachments(attachments: List<AttachmentEntity>)
    
    @Query("SELECT * FROM attachment WHERE messageId = :messageId")
    suspend fun getAttachmentsByMessageId(messageId: Long): List<AttachmentEntity>
}
