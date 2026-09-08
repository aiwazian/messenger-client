package com.aiwazian.messenger.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "sticker_pack")
data class StickerPackEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val username: String,
    val ownerId: Long,
    val stickerCount: Int,
    val isOwned: Boolean,
    val isInstalled: Boolean,
    val sortOrder: Int = 0
)
