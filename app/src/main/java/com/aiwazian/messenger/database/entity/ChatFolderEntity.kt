/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "chat_folders")
data class ChatFolderEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val sortOrder: Int,
    /**
     * Категории хранятся строкой через запятую, а не отдельной таблицей:
     * их не больше трёх и по ним никогда не ищут.
     */
    val categories: String
)
