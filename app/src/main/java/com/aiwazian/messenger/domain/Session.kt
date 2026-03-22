/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class Session(
    val id: Int = 0,
    val userId: Int = 0,
    val deviceModel: String = "",
    val osVersion: String = "",
    val osName: String = "",
    val createdAt: Long = 0
)
