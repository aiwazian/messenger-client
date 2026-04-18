/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class User(
    val id: Long,
    val firstName: String,
    val lastName: String?,
    val username: String?,
    val bio: String?,
    val dateOfBirth: Long?,
    val lastSeen: Long?
)
