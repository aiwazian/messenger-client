/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class Session(
    val id: Int,
    val userId: Long,
    val deviceModel: String,
    val osVersion: String,
    val osName: String,
    val createdAt: Long,
    val isCurrent: Boolean
)
