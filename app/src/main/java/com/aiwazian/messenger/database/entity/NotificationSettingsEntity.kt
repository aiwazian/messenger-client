/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "notification_settings")
data class NotificationSettingsEntity(
    @PrimaryKey
    val userId: Long,
    val privateChats: Boolean = true,
    val groups: Boolean = true,
    val channels: Boolean = true
)
