/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.socket

sealed class ConnectionEvent {
    data object Connected : ConnectionEvent()
    data class Disconnected(
        val code: Int,
        val reason: String
    ) : ConnectionEvent()
    
    data class Error(val error: WebSocketError) : ConnectionEvent()
}