/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Настройки уведомлений отдельно для каждого аккаунта на устройстве.
 *
 * Ключ — userId, а не единственная строка: аккаунтов может быть несколько, и при
 * переключении настройки не должны переезжать вместе с пользователем.
 *
 * Кэш серверного состояния, а не источник правды: нужен, чтобы экран открывался без
 * мигания и чтобы пуш можно было отфильтровать без сети.
 */
@Entity(tableName = "notification_settings")
data class NotificationSettingsEntity(
    @PrimaryKey
    val userId: Long,
    val privateChats: Boolean = true,
    val groups: Boolean = true,
    val channels: Boolean = true
)
