/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.Entity

@Entity(
    tableName = "drafts",
    primaryKeys = ["userId", "chatId"]
)
data class DraftEntity(
    val userId: Long,
    val chatId: Long,
    val text: String
)
