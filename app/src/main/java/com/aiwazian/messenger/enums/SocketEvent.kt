/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SocketEvent {
    @SerialName("message:new")
    MESSAGE_NEW,

    @SerialName("message:edit")
    MESSAGE_EDIT,

    @SerialName("message:update")
    MESSAGE_UPDATE,

    @SerialName("message:delete")
    MESSAGE_DELETE,

    @SerialName("chat:typing")
    CHAT_TYPING,

    @SerialName("chat:open")
    CHAT_OPEN,

    @SerialName("chat:close")
    CHAT_CLOSE,

    @SerialName("chat:read")
    CHAT_READ,

    @SerialName("chat:new")
    CHAT_NEW,

    @SerialName("chat:history_clear")
    HISTORY_CLEAR,

    @SerialName("user:online")
    USER_ONLINE,

    @SerialName("user:offline")
    USER_OFFLINE,

    @SerialName("auth:error")
    AUTH_ERROR;
}
