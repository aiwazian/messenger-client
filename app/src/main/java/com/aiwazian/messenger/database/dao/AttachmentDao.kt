/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.dao

import androidx.room3.Dao
import androidx.room3.Embedded
import androidx.room3.Query
import androidx.room3.Upsert
import com.aiwazian.messenger.database.entity.AttachmentEntity

data class ViewOnceAttachmentRecord(
    @Embedded val attachment: AttachmentEntity,
    val messageSendTime: Long
)

@Dao
interface AttachmentDao {
    @Upsert
    suspend fun upsertAttachments(attachments: List<AttachmentEntity>)
    
    @Query("SELECT * FROM attachment WHERE messageId = :messageId")
    suspend fun getAttachmentsByMessageId(messageId: Long): List<AttachmentEntity>

    @Query("DELETE FROM attachment WHERE messageId = :messageId AND fileId = :fileId")
    suspend fun deleteAttachment(messageId: Long, fileId: String)

    @Query("SELECT COUNT(*) FROM attachment WHERE fileId = :fileId")
    suspend fun countByFileId(fileId: String): Int

    @Query(
        """
        SELECT attachment.*, message.sendTime AS messageSendTime
        FROM attachment
        INNER JOIN message ON message.id = attachment.messageId
        WHERE attachment.viewOnce = 1
            AND message.status = 'SENT'
            AND (
                (message.senderId = :myId AND message.chatId = :chatId)
                OR (message.chatId = :myId AND message.senderId = :chatId)
            )
            AND message.ownerId = (
                SELECT userId FROM account WHERE isCurrent = 1 ORDER BY id DESC LIMIT 1
            )
        """
    )
    suspend fun getConfirmedViewOnceAttachmentsForPrivateChat(
        myId: Long,
        chatId: Long
    ): List<ViewOnceAttachmentRecord>
}
