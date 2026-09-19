/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity


@Entity(
    tableName = "chat_media_counts",
    primaryKeys = ["chatId", "ownerId"]
)
data class ChatMediaCountsEntity(
    val chatId: Long,
    val photos: Int,
    val videos: Int,
    val files: Int,
    @ColumnInfo(defaultValue = "0")
    val music: Int,
    val voices: Int,
    @ColumnInfo(defaultValue = "0")
    val ownerId: Long = 0
)
