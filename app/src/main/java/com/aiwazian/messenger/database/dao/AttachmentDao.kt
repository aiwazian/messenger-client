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
import com.aiwazian.messenger.enums.AttachmentType
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAttachments(attachments: List<AttachmentEntity>)

    @Transaction
    suspend fun upsertAttachments(attachments: List<AttachmentEntity>) {
        attachments.forEach { newAttachment ->
            val existing = getAttachmentById(newAttachment.id)
            if (existing != null) {
                val updated = newAttachment.copy(
                    status = if (newAttachment.status == com.aiwazian.messenger.domain.DownloadStatus.IDLE && existing.status != com.aiwazian.messenger.domain.DownloadStatus.IDLE) existing.status else newAttachment.status,
                    progress = if (newAttachment.progress == 0 && existing.progress != 0) existing.progress else newAttachment.progress,
                    localUri = newAttachment.localUri ?: existing.localUri
                )
                saveAttachment(updated)
            } else {
                saveAttachment(newAttachment)
            }
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAttachment(attachment: AttachmentEntity)

    @Query("SELECT * FROM attachment WHERE relationId = :relationId AND type = :type")
    suspend fun getAttachments(relationId: Long, type: AttachmentType): List<AttachmentEntity>

    @Query("SELECT * FROM attachment WHERE relationId = :relationId AND type = :type")
    fun getAttachmentsFlow(relationId: Long, type: AttachmentType): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachment WHERE id = :id")
    suspend fun getAttachmentById(id: String): AttachmentEntity?

    @Query("UPDATE attachment SET status = :status, progress = :progress, localUri = :localUri WHERE id = :id")
    suspend fun updateAttachmentStatus(id: String, status: com.aiwazian.messenger.domain.DownloadStatus, progress: Int, localUri: String?)

    @Query("DELETE FROM attachment WHERE id = :id")
    suspend fun deleteAttachment(id: String)

    @Query("DELETE FROM attachment WHERE relationId = :relationId AND type = :type")
    suspend fun deleteAttachmentsByRelation(relationId: Long, type: AttachmentType)

    @Query("SELECT * FROM attachment")
    suspend fun getAllAttachments(): List<AttachmentEntity>
}
