/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.socket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WebSocketAction(val value: String) {
    @SerialName("message:new")
    NEW_MESSAGE("message:new"),
    
    @SerialName("message:edit")
    MESSAGE_EDIT("message:edit"),
    
    @SerialName("message:update")
    MESSAGE_UPDATE("message:update"),
    
    @SerialName("message:delete")
    DELETE_MESSAGE("message:delete"),
    
    @SerialName("chat:typing")
    CHAT_TYPING("chat:typing"),
    
    @SerialName("chat:open")
    CHAT_OPEN("chat:open"),
    
    @SerialName("chat:close")
    CHAT_CLOSE("chat:close"),
    
    @SerialName("chat:read")
    CHAT_READ("chat:read"),
    
    @SerialName("chat:new")
    NEW_CHAT("chat:new"),
    
    @SerialName("chat:history_clear")
    HISTORY_CLEAR("chat:history_clear"),
    
    @SerialName("user:online")
    USER_ONLINE("user:online"),
    
    @SerialName("user:offline")
    USER_OFFLINE("user:offline"),
    
    @SerialName("auth:error")
    AUTH_ERROR("auth:error"),
    
    @SerialName("delete_chat")
    DELETE_CHAT("delete_chat"),
    
    @SerialName("read_message")
    READ_MESSAGE("read_message");
    
    companion object {
        private val map = entries.associateBy { it.value }
        
        fun from(value: String): WebSocketAction? {
            return map[value]
        }
    }
}