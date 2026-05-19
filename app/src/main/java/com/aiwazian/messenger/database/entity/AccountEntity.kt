/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "account",
    indices = [
        Index(value = ["token"], unique = true),
        Index(value = ["userId"], unique = true)
    ]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Long,
    val isCurrent: Boolean,
    val token: String = "",
    val fcmToken: String? = null,
    val createdAt: Long = 0
)
