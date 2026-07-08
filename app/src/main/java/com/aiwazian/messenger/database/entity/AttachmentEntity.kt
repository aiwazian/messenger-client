/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.aiwazian.messenger.enums.AttachmentType

@Entity(
    tableName = "attachment",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FileEntity::class,
            parentColumns = ["id"],
            childColumns = ["fileId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["messageId"]),
        Index(value = ["fileId"]),
        Index(value = ["messageId", "fileId"], unique = true)
    ]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileId: String,
    val messageId: Long,
    val type: AttachmentType,
    val sortOrder: Int
)
