/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.mappers

import com.aiwazian.messenger.network.dto.MessageResponseDto
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.MessageFile
import com.aiwazian.messenger.database.entity.MessageEntity
import com.aiwazian.messenger.network.api.RetrofitInstance
import com.aiwazian.messenger.network.dto.MessageFileDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

fun MessageResponseDto.toDomain(): Message = Message(
    id = this.id,
    senderId = this.senderId.toLong(),
    chatId = this.chatId.toLong(),
    text = this.text,
    sendTime = this.sendTime,
    isRead = this.isRead ?: false,
    files = this.files.map { it.toDomain() }
)

fun MessageFileDto.toDomain(): MessageFile = MessageFile(
    id = this.id,
    name = this.name,
    size = try { this.size.toLong() } catch (e: Exception) { 0L },
    extension = this.name.substringAfterLast('.', "")
)

fun MessageEntity.toDomain(): Message = Message(
    id = this.id,
    senderId = this.senderId,
    chatId = this.chatId,
    text = this.text,
    sendTime = this.sendTime,
    isRead = this.isRead,
    files = this.filesJson?.let {
        try {
            RetrofitInstance.json.decodeFromString<List<MessageFile>>(it)
        } catch (e: Exception) {
            emptyList()
        }
    } ?: emptyList()
)

fun Message.toEntity(): MessageEntity {
    return MessageEntity(
        id = this.id,
        senderId = this.senderId,
        chatId = this.chatId,
        text = this.text,
        sendTime = this.sendTime,
        isRead = this.isRead,
        filesJson = RetrofitInstance.json.encodeToString(this.files)
    )
}
