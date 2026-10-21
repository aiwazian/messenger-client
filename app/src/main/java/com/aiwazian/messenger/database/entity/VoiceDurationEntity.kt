/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "voice_duration")
data class VoiceDurationEntity(
    @PrimaryKey
    val fileId: String,
    val durationMs: Int
)
