package com.aiwazian.messenger.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("attachment")
data class AttachmentEntity(
    @PrimaryKey val id: Int,
    val messageId: Int,
    val name: String,
    val url: String,
    val size: Long
)
