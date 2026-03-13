package com.aiwazian.messenger.data

import androidx.annotation.Keep
import com.aiwazian.messenger.enums.WebSocketAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Keep
@Serializable
data class WebSocketMessage(
    @Keep val action: WebSocketAction,
    @Keep val data: JsonObject
)
