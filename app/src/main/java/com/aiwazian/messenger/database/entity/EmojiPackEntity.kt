package com.aiwazian.messenger.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "emoji_pack")
data class EmojiPackEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val username: String,
    val ownerId: Long,
    val emojiCount: Int,
    val isOwned: Boolean,
    val isInstalled: Boolean,
    val sortOrder: Int = 0
)
