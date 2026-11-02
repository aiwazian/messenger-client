/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.aiwazian.messenger.enums.ProxyType

@Entity(tableName = "proxy")
data class ProxyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: ProxyType,
    val host: String,
    val port: Int,
    val username: String?,
    val password: String?
)
