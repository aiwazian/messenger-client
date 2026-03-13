package com.aiwazian.messenger.database.mappers

import com.aiwazian.messenger.data.Message
import com.aiwazian.messenger.database.entity.MessageEntity

fun Message.toEntity(): MessageEntity {
    return MessageEntity(
        id = this.id,
        senderId = this.senderId,
        chatId = this.chatId,
        text = this.text,
        sendTime = this.sendTime,
        isRead = this.isRead
    )
}

fun MessageEntity.toMessage(): Message {
    return Message(
        id = this.id,
        senderId = this.senderId,
        chatId = this.chatId,
        text = this.text,
        sendTime = this.sendTime,
        isRead = this.isRead
    )
}
