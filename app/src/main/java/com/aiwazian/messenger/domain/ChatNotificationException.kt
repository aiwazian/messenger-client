/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

/**
 * Чат, выбитый из общего правила: уведомления по нему всегда включены или всегда
 * выключены, что бы ни стояло у его категории.
 *
 * Нужен будущему экрану со списком исключений: там важен именно факт исключения
 * и его направление, а не итоговое состояние чата, которое едет в Chat.isMuted.
 */
data class ChatNotificationException(
    val chatId: Long,
    /** true — чат принудительно со звуком, false — принудительно молчит. */
    val enabled: Boolean
)
