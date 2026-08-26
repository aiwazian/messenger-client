/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.Entity

/**
 * Сколько вложений в чате всего — для подписи в шапке галереи.
 *
 * Счётчики не выводятся из [ChatMediaEntity]: там лежит только первое окно
 * списка, а подпись говорит о всём чате. Кэшируются затем же, зачем и сам
 * список: чтобы при открытии без сети шапка не была полупустой.
 *
 * Ключ составной: в личном чате идентификатор собеседника один и тот же для
 * всех аккаунтов на устройстве, а счётчики у каждого свои.
 */
@Entity(
    tableName = "chat_media_counts",
    primaryKeys = ["chatId", "ownerId"]
)
data class ChatMediaCountsEntity(
    val chatId: Long,
    val photos: Int,
    val videos: Int,
    val files: Int,
    val voices: Int,
    val ownerId: Long = 0
)
