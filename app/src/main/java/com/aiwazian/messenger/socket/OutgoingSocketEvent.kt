/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.socket

/**
 * Имена событий, которые отправляет сам клиент.
 *
 * Входящие события живут в WebSocketEvent вместе со своими DTO, а здесь нужны только имена:
 * раньше "chat_open" было вписано строкой прямо в ChatViewModel.
 */
object OutgoingSocketEvent {
    const val CHAT_OPEN = "chat_open"
}
