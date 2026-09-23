package com.aiwazian.messenger.domain

import kotlinx.serialization.Serializable

/**
 * Закреплённое сообщение так, как его видит текущий пользователь.
 *
 * forEveryone = false — закреплено «для меня», forEveryone = true — «для всех».
 */
data class PinnedMessage(
    val messageId: Long,
    val chatId: Long,
    val forEveryone: Boolean,
    val pinnedAt: Long,
    val message: Message?
)

/**
 * Payload события message:pin / message:unpin.
 *
 * chatId — «чат» в терминах UI получателя события: в личном чате сервер
 * подставляет собеседника для каждой из сторон.
 */
@Serializable
data class MessagePinPayload(
    val chatId: Long,
    val messageId: Long,
    val forEveryone: Boolean = false,
    val pinnedAt: Long = 0
)
