/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.socket

import com.aiwazian.messenger.domain.Chat
import com.aiwazian.messenger.domain.ChatFolder
import com.aiwazian.messenger.domain.ChatFolderDeletedPayload
import com.aiwazian.messenger.domain.ChatUnreadPayload
import com.aiwazian.messenger.domain.DeleteChatPayload
import com.aiwazian.messenger.domain.DeleteMessagePayload
import com.aiwazian.messenger.domain.Message
import com.aiwazian.messenger.domain.PinChatPayload
import com.aiwazian.messenger.domain.PresencePayload
import com.aiwazian.messenger.domain.ReadMessagePayload
import com.aiwazian.messenger.mappers.toDomain
import com.aiwazian.messenger.network.dto.ChatFolderDto
import com.aiwazian.messenger.network.dto.ChatResponseDto
import com.aiwazian.messenger.network.dto.MessageDto
import kotlinx.serialization.DeserializationStrategy

sealed interface WebSocketEvent<Dto : Any, Domain : Any> {
    val eventName: String
    val deserializer: DeserializationStrategy<Dto>
    val mapper: (Dto) -> Domain
    
    data object NewMessage : WebSocketEvent<MessageDto, Message> {
        override val eventName = "message:new"
        override val deserializer = MessageDto.serializer()
        override val mapper: (MessageDto) -> Message = MessageDto::toDomain
    }
    
    data object MessageEdit : WebSocketEvent<MessageDto, Message> {
        override val eventName = "message:edit"
        override val deserializer = MessageDto.serializer()
        override val mapper: (MessageDto) -> Message = MessageDto::toDomain
    }
    
    data object DeleteMessage : WebSocketEvent<DeleteMessagePayload, DeleteMessagePayload> {
        override val eventName = "message:delete"
        override val deserializer = DeleteMessagePayload.serializer()
        override val mapper: (DeleteMessagePayload) -> DeleteMessagePayload = { it }
    }
    
    data object NewChat : WebSocketEvent<ChatResponseDto, Chat> {
        override val eventName = "chat:new"
        override val deserializer = ChatResponseDto.serializer()
        override val mapper: (ChatResponseDto) -> Chat = ChatResponseDto::toDomain
    }
    
    data object ChatRemoved : WebSocketEvent<DeleteChatPayload, DeleteChatPayload> {
        override val eventName = "chat:removed"
        override val deserializer = DeleteChatPayload.serializer()
        override val mapper: (DeleteChatPayload) -> DeleteChatPayload = { it }
    }
    
    data object HistoryClear : WebSocketEvent<DeleteChatPayload, DeleteChatPayload> {
        override val eventName = "chat:history_clear"
        override val deserializer = DeleteChatPayload.serializer()
        override val mapper: (DeleteChatPayload) -> DeleteChatPayload = { it }
    }
    
    data object ReadMessage : WebSocketEvent<ReadMessagePayload, ReadMessagePayload> {
        override val eventName = "read_message"
        override val deserializer = ReadMessagePayload.serializer()
        override val mapper: (ReadMessagePayload) -> ReadMessagePayload = { it }
    }
    
    data object ChatRead : WebSocketEvent<ReadMessagePayload, ReadMessagePayload> {
        override val eventName = "chat:read"
        override val deserializer = ReadMessagePayload.serializer()
        override val mapper: (ReadMessagePayload) -> ReadMessagePayload = { it }
    }
    
    data object ChatUnread : WebSocketEvent<ChatUnreadPayload, ChatUnreadPayload> {
        override val eventName = "chat:unread"
        override val deserializer = ChatUnreadPayload.serializer()
        override val mapper: (ChatUnreadPayload) -> ChatUnreadPayload = { it }
    }
    
    data object UserOnline : WebSocketEvent<PresencePayload, PresencePayload> {
        override val eventName = "user:online"
        override val deserializer = PresencePayload.serializer()
        override val mapper: (PresencePayload) -> PresencePayload = { it }
    }
    
    data object UserOffline : WebSocketEvent<PresencePayload, PresencePayload> {
        override val eventName = "user:offline"
        override val deserializer = PresencePayload.serializer()
        override val mapper: (PresencePayload) -> PresencePayload = { it }
    }
    
    data object PinChat : WebSocketEvent<PinChatPayload, PinChatPayload> {
        override val eventName = "pin_chat"
        override val deserializer = PinChatPayload.serializer()
        override val mapper: (PinChatPayload) -> PinChatPayload = { it }
    }
    
    data object UnpinChat : WebSocketEvent<PinChatPayload, PinChatPayload> {
        override val eventName = "unpin_chat"
        override val deserializer = PinChatPayload.serializer()
        override val mapper: (PinChatPayload) -> PinChatPayload = { it }
    }
    
    data object ChatFolderNew : WebSocketEvent<ChatFolderDto, ChatFolder> {
        override val eventName = "chat_folder:new"
        override val deserializer = ChatFolderDto.serializer()
        override val mapper: (ChatFolderDto) -> ChatFolder = ChatFolderDto::toDomain
    }
    
    data object ChatFolderDeleted :
        WebSocketEvent<ChatFolderDeletedPayload, ChatFolderDeletedPayload> {
        override val eventName = "chat_folder:deleted"
        override val deserializer = ChatFolderDeletedPayload.serializer()
        override val mapper: (ChatFolderDeletedPayload) -> ChatFolderDeletedPayload = { it }
    }
}
