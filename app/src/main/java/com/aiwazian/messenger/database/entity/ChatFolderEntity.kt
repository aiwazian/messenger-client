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
    /** Папки принадлежат аккаунту: при его смене кэш чужой учётки не показывается. */
    @ColumnInfo(defaultValue = "0") val userId: Long,
    val id: Int,
    val name: String,
    val sortOrder: Int,
    /**
     * Категории хранятся строкой через запятую, а не отдельной таблицей:
     * их не больше трёх и по ним никогда не ищут.
     */
    val categories: String
)
