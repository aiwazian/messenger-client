/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity

@Entity(
    tableName = "chat_folders",
    primaryKeys = ["userId", "id"]
)
data class ChatFolderEntity(
    @ColumnInfo(defaultValue = "0") val userId: Long,
    val id: Int,
    val name: String,
    val sortOrder: Int,
    val categories: String
)
