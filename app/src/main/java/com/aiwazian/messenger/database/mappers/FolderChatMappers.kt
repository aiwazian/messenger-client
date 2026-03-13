package com.aiwazian.messenger.database.mappers

import com.aiwazian.messenger.data.ChatInfo
import com.aiwazian.messenger.database.entity.FolderChatEntity
import com.aiwazian.messenger.enums.ChatType

fun ChatInfo.toEntity(
    folderId: Int,
    lastMessageId: Int? = null
): FolderChatEntity {
    return FolderChatEntity(
        id = this.id,
        chatName = this.chatName,
        isPinned = this.isPinned,
        folderId = folderId,
        lastMessageId = lastMessageId
    )
}

fun FolderChatEntity.toChat(): ChatInfo {
    return ChatInfo(
        id = this.id,
        chatName = this.chatName,
        isPinned = this.isPinned
    )
}
