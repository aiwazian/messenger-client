/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

@Entity(
    tableName = "chat_folder_chats",
    primaryKeys = ["folderId", "chatId"],
    foreignKeys = [ForeignKey(
        entity = ChatFolderEntity::class,
        parentColumns = ["id"],
        childColumns = ["folderId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("folderId")]
)
data class ChatFolderChatEntity(
    val folderId: Int,
    val chatId: Long,
    /** Чат добавлен в папку поимённо, а не попал в неё через категорию. */
    val isIncluded: Boolean,
    /** Закрепление внутри этой папки, независимое от остальных. */
    val isPinned: Boolean,
    val sortOrder: Int
)
