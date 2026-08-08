/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

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
    /** Последний Firebase Installation ID, который уже отправлен на сервер. */
    val installationId: String? = null,
    val createdAt: Long = 0
)
