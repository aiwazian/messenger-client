package com.aiwazian.messenger.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("folderChat")
data class FolderChatEntity(
    @PrimaryKey var id: Long,
    val folderId: Int = 0,
    var chatName: String = "",
    var isPinned: Boolean = false,
    var lastMessageId: Int? = null
)