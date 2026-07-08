/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.aiwazian.messenger.database.entity.AttachmentEntity

@Dao
interface AttachmentDao {
    @Upsert
    suspend fun upsertAttachments(attachments: List<AttachmentEntity>)
    
    @Query("SELECT * FROM attachment WHERE messageId = :messageId")
    suspend fun getAttachmentsByMessageId(messageId: Long): List<AttachmentEntity>
}
