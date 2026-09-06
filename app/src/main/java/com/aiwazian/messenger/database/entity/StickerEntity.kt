package com.aiwazian.messenger.database.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "sticker",
    indices = [Index(value = ["packId"])]
)
data class StickerEntity(
    @PrimaryKey val id: Long,
    val packId: Long,
    val fileId: String,
    val url: String,
    val sortOrder: Int,
    val emojis: String
)
