/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.domain

data class User(
    val id: Long = 0,
    val firstName: String = "",
    val lastName: String? = null,
    val username: String? = null,
    val bio: String? = null,
    val dateOfBirth: Long? = null,
)
